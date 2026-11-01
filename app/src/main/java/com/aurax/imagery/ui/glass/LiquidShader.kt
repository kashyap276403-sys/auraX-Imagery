package com.aurax.imagery.ui.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * AGSL shader that turns a flat blurred backdrop into liquid glass. The `content` input is the
 * panel's own backdrop pixels (what Haze captured behind it and blurred), so the effect
 * refracts what is genuinely behind the glass.
 *
 * Effects, all derived from a signed-distance field of the rounded panel:
 *  - refraction: pixels near the rim are sampled from further inside, like a thick lens edge.
 *    Capped to a fraction of the bevel so shapes stay recognisable.
 *  - chromatic separation: red and blue are sampled slightly apart along the edge normal.
 *  - edge light: a bright rim facing a light direction, plus a faint counter-rim.
 *  - reflection: a soft diagonal sheen band that drifts with device tilt.
 *
 * The shader only touches the backdrop layer, never the icons and text drawn on top of it, so
 * distortion cannot hurt legibility.
 */
class LiquidShader {
    // Constructing a RuntimeShader compiles the AGSL; an exception here is handled by callers
    // (they fall back to plain blurred glass).
    private val shader = RuntimeShader(AGSL)

    private var last: Params? = null
    private var lastEffect: androidx.compose.ui.graphics.RenderEffect? = null

    private data class Params(
        val w: Float, val h: Float, val radius: Float,
        val refraction: Float, val edge: Float, val reflection: Float,
        val tiltX: Float, val tiltY: Float,
    )

    /**
     * Returns the effect for these parameters. The same RuntimeShader (and so the same compiled
     * program) is reused; only uniforms change, and a new RenderEffect is made only when
     * a parameter actually changed.
     */
    fun effect(
        width: Float,
        height: Float,
        radiusPx: Float,
        refraction: Float,
        edgeBrightness: Float,
        reflection: Float,
        tiltX: Float,
        tiltY: Float,
    ): androidx.compose.ui.graphics.RenderEffect {
        val params = Params(width, height, radiusPx, refraction, edgeBrightness, reflection, tiltX, tiltY)
        val cached = lastEffect
        if (cached != null && params == last) return cached

        shader.setFloatUniform("uSize", width, height)
        shader.setFloatUniform("uRadius", radiusPx)
        shader.setFloatUniform("uRefraction", refraction)
        // Chromatic separation follows refraction but stays soft.
        shader.setFloatUniform("uChroma", refraction * 0.6f)
        shader.setFloatUniform("uEdge", edgeBrightness)
        shader.setFloatUniform("uReflection", reflection)
        shader.setFloatUniform("uTilt", tiltX, tiltY)
        val effect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        last = params
        lastEffect = effect
        return effect
    }

    companion object {
        private const val AGSL = """
uniform shader content;
uniform float2 uSize;
uniform float uRadius;
uniform float uRefraction;
uniform float uChroma;
uniform float uEdge;
uniform float uReflection;
uniform float2 uTilt;

// Signed distance to a rounded box centred on the origin: negative inside, 0 on the edge.
float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

half4 main(float2 coord) {
    float2 halfSize = uSize * 0.5;
    float r = min(uRadius, min(halfSize.x, halfSize.y));
    float2 p = coord - halfSize;
    float d = sdRoundBox(p, halfSize, r);

    // Width of the curved "bevel" at the rim where light bends.
    float bevel = max(6.0, min(uSize.x, uSize.y) * 0.30);
    float edge = 1.0 - smoothstep(0.0, bevel, -d);   // 1 on the rim, 0 once inside the bevel

    // Outward surface normal from the gradient of the distance field.
    float dx = sdRoundBox(p + float2(1.0, 0.0), halfSize, r) - sdRoundBox(p - float2(1.0, 0.0), halfSize, r);
    float dy = sdRoundBox(p + float2(0.0, 1.0), halfSize, r) - sdRoundBox(p - float2(0.0, 1.0), halfSize, r);
    float2 n = float2(dx, dy);
    float nl = length(n);
    n = nl > 0.0001 ? n / nl : float2(0.0, -1.0);

    // Refraction: sample from further inside near the rim. Quadratic falloff keeps the middle flat.
    float shift = uRefraction * edge * edge * bevel * 0.85;
    float2 uv = coord - n * shift;

    // Soft chromatic separation along the normal.
    float ca = uChroma * edge * bevel * 0.30;
    float2 lo = float2(0.5, 0.5);
    float2 hi = uSize - float2(0.5, 0.5);
    half4 cr = content.eval(clamp(uv - n * ca, lo, hi));
    half4 cg = content.eval(clamp(uv, lo, hi));
    half4 cb = content.eval(clamp(uv + n * ca, lo, hi));
    half4 c = half4(cr.r, cg.g, cb.b, cg.a);

    // Edge light from the upper left; tilting the phone moves the light.
    float2 lightDir = normalize(float2(-0.55, -0.85) + uTilt * 0.6);
    float facing = dot(n, lightDir);
    float rim = edge * edge;
    float spec = pow(max(facing, 0.0), 2.0) * rim;
    float counter = pow(max(-facing, 0.0), 3.0) * rim * 0.35;
    float light = (spec + counter) * uEdge * 0.9;

    // Dynamic reflection: a diagonal sheen band that slides with tilt.
    float t = coord.x / uSize.x + (coord.y / uSize.y) * 0.6;
    float centre = 0.38 + uTilt.x * 0.25 + uTilt.y * 0.12;
    float band = clamp(1.0 - abs(t - centre) * 3.2, 0.0, 1.0);
    float sheen = band * band * uReflection * 0.10 * (1.0 - edge * 0.5);

    c.rgb = clamp(c.rgb + half3(light + sheen) * c.a, half3(0.0), half3(1.0));
    return c;
}
"""
    }
}
