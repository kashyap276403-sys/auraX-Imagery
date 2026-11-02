# Video player: device test checklist

Nothing below has been run on a phone. CI only compiles the app and runs the JVM unit tests.
Tick each line on a real device and note the phone model and Android version.

Install: GitHub > Actions > latest "Build" run > artifact `aurax-imagery-debug` > unzip > install the APK.

## Display and zoom
- [ ] Portrait, landscape, 16:9, 21:9, 4:3 and 1:1 videos open without distortion in Fit
- [ ] Fit shows bars and never crops; Fill crops and fills; Original never enlarges; Stretch fills and distorts
- [ ] Aspect presets change the frame in Fit and Fill, and are disabled in Original and Stretch
- [ ] Pinch zooms 1x-4x; one finger pans only while zoomed; double-tap resets; slider and Reset in the ratio sheet agree
- [ ] Last mode and preset are remembered after closing and reopening the app (Settings > Remember last screen mode on)
- [ ] Display cutout and gesture bar do not cover controls in landscape

## Controls and gestures
- [ ] Controls hide after the Settings delay while playing and return on tap (also after rotation)
- [ ] Double-tap left/right seeks by the chosen interval; centre double-tap resets zoom
- [ ] Left swipe changes brightness, right swipe changes media volume; both restore/behave when leaving
- [ ] With a gesture switched off, swiping down on that half closes the video
- [ ] Play/pause, rewind, forward, seek bar drag and the frame preview work
- [ ] Bottom favourite/share/delete bar is hidden while playing and in landscape, shown when paused in portrait
- [ ] Speed 0.25x-2x, rotation lock Auto/Portrait/Landscape, fullscreen button

## Subtitles, audio, smart playback
- [ ] Embedded subtitle tracks list and switch; Off works; size and position chips work
- [ ] "Add .srt file" loads an external SRT and shows its lines; a non-.srt file is refused with a message
- [ ] Audio track switching on a multi-audio file; unsupported tracks are greyed
- [ ] A-B repeat: set A, set B, loop, clear; yellow range on the seek bar
- [ ] Repeat one / queue, next/previous, autoplay next, remove from queue, restore
- [ ] Bookmarks: add with and without a name, jump, delete

## Resume, history, extras
- [ ] Leave a video halfway, reopen: "Continuing from..." with Start over; finished videos start at 0
- [ ] Albums tab shows Continue watching and Recently played; Settings > Clear watch history empties them
- [ ] Sleep timer pauses at the end; custom minutes; turning it off
- [ ] Picture-in-picture from the menu and by pressing Home while playing; play/pause button in the PiP window; tapping the window returns to full screen
- [ ] Screen off or leaving the app pauses playback (not in PiP)
- [ ] Save frame as photo appears in Pictures/AuraX Imagery
- [ ] Video details shows resolution, FPS, codec, bitrate, location
- [ ] Cinematic looks change colour; Original restores it; saved frames are unaffected

## Robustness
- [ ] First launch after updating from an older APK keeps favourites and the media index (database migration 1 -> 3)
- [ ] A corrupt or unsupported video shows "This video can't be played" and does not crash
- [ ] Revoking media permission in system settings while the app is open does not crash
- [ ] Large video (several GB) opens and seeks; a library of thousands of items scrolls smoothly
- [ ] TalkBack: buttons are announced, the seek bar reads "1:23 of 4:56", levels are announced
- [ ] Animations off in system settings: controls appear without fades
- [ ] Photos still open, zoom, edit, share and delete as before
