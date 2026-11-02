package com.aurax.imagery.ui.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * AGSL shader for the Water Drop Liquid Glass style. Like [LiquidShader] it only touches the
 * panel's own backdrop pixels, never the icons and text drawn above, so legibility is unaffected.
 *
 * The panel is treated as a domed droplet of water, using the signed distance to its rounded box
 * as a height field:
 *  - lens refraction: pixels in the wide curved rim are sampled from further inside (strong near
 *    the edge, easing to zero), and the flat centre is magnified very slightly.
 *  - dispersion: red and blue are sampled apart along the surface normal.
 *  - specular: a bright crescent on the light-facing rim plus a small soft glint just inside it.
 *  - caustic: light gathered on the opposite inner rim, as in a real droplet.
 *  - fresnel: the very edge darkens a little, which gives the rim visible thickness.
 *  - gloss: a faint curved sheen over the upper part.
 * Light direction follows device tilt. [strength] (0..1) scales every one of these.
 */
class WaterDropShader {
    private val shader = RuntimeShader(AGSL)

    private var last: Params? = null
    private var lastEffect: androidx.compose.ui.graphics.RenderEffect? = null

    private data class Params(
        val w: Float, val h: Float, val radius: Float,
        val strength: Float, val refraction: Float, val edge: Float, val gloss: Float,
        val tiltX: Float, val tiltY: Float,
    )

    fun effect(
        width: Float,
        height: Float,
        radiusPx: Float,
        strength: Float,
        refraction: Float,
        edgeBrightness: Float,
        gloss: Float,
        tiltX: Float,
        tiltY: Float,
    ): androidx.compose.ui.graphics.RenderEffect {
        val params = Params(width, height, radiusPx, strength, refraction, edgeBrightness, gloss, tiltX, tiltY)
        val cached = lastEffect
        if (cached != null && params == last) return cached

        shader.setFloatUniform("uSize", width, height)
        shader.setFloatUniform("uRadius", radiusPx)
        shader.setFloatUniform("uStrength", strength)
        shader.setFloatUniform("uRefraction", refraction)
        shader.setFloatUniform("uEdge", edgeBrightness)
        shader.setFloatUniform("uGloss", gloss)
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
uniform float uStrength;
uniform float uRefraction;
uniform float uEdge;
uniform float uGloss;
uniform float2 uTilt;

float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

half4 main(float2 coord) {
    float2 halfSize = uSize * 0.5;
    float r = min(uRadius, min(halfSize.x, halfSize.y));
    float2 p = coord - halfSize;
    float d = sdRoundBox(p, halfSize, r);

    // A wide, curved rim: the droplet's dome. t is 0 on the edge and 1 once fully inside it.
    float bevel = max(8.0, min(uSize.x, uSize.y) * 0.42);
    float t = clamp(-d / bevel, 0.0, 1.0);
    float edge = 1.0 - t;

    float dx = sdRoundBox(p + float2(1.0, 0.0), halfSize, r) - sdRoundBox(p - float2(1.0, 0.0), halfSize, r);
    float dy = sdRoundBox(p + float2(0.0, 1.0), halfSize, r) - sdRoundBox(p - float2(0.0, 1.0), halfSize, r);
    float2 n = float2(dx, dy);
    float nl = length(n);
    n = nl > 0.0001 ? n / nl : float2(0.0, -1.0);

    // Lens refraction: steep near the rim, easing off toward the flat middle.
    float k = uStrength * (0.55 + 0.45 * uRefraction);
    float shift = k * bevel * 0.95 * pow(edge, 1.7);
    float2 uv = coord - n * shift;
    // Slight magnification of the centre, as a water drop does.
    uv = halfSize + (uv - halfSize) * (1.0 - 0.05 * k * t);

    float ca = k * edge * bevel * 0.16;
    float2 lo = float2(0.5, 0.5);
    float2 hi = uSize - float2(0.5, 0.5);
    half4 cr = content.eval(clamp(uv - n * ca, lo, hi));
    half4 cg = content.eval(clamp(uv, lo, hi));
    half4 cb = content.eval(clamp(uv + n * ca, lo, hi));
    half4 c = half4(cr.r, cg.g, cb.b, cg.a);

    float2 lightDir = normalize(float2(-0.6, -0.8) + uTilt * 0.7);
    float facing = dot(n, lightDir);

    // Rim thickness: the very edge is a touch darker than what is behind it.
    float fres = pow(edge, 4.0);
    c.rgb *= 1.0 - 0.16 * k * fres;

    // Specular crescent on the light side, narrow and bright.
    float crescent = pow(max(facing, 0.0), 5.0) * smoothstep(0.0, 0.18, edge) * (1.0 - smoothstep(0.35, 0.8, edge));
    // Soft glint hugging the crescent, a little further inside.
    float glint = pow(max(facing, 0.0), 3.0) * smoothstep(0.2, 0.45, edge) * (1.0 - smoothstep(0.55, 1.0, edge)) * 0.35;
    // Caustic: light focused onto the opposite inner rim.
    float caustic = pow(max(-facing, 0.0), 2.5) * smoothstep(0.1, 0.35, edge) * (1.0 - smoothstep(0.45, 0.9, edge));

    float light = (crescent * 1.0 + glint) * uEdge * k * 1.25 + caustic * k * 0.30;

    // Curved gloss over the upper part, moving with tilt.
    float v = coord.y / uSize.y;
    float u = coord.x / uSize.x;
    float arc = smoothstep(0.55, 0.0, v + 0.18 * (u - 0.5 - uTilt.x * 0.3) * (u - 0.5 - uTilt.x * 0.3) * 4.0 - uTilt.y * 0.05);
    float sheen = arc * uGloss * k * 0.13;

    c.rgb = clamp(c.rgb + half3(light + sheen) * c.a, half3(0.0), half3(1.0));
    return c;
}
"""
    }
}
