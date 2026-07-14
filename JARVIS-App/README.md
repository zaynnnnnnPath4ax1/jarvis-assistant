# JARVIS App

Single zip, poora Android project andar hai — same ZAYN/JARVIS UI jo tune bheji thi, ab real phone control se juda hua hai.

## Is version me kya real hai
- **Voice control**: mic dabao, bolo ("open whatsapp", "go home", "go back",
  "recents", "volume up/down") — Android ka native SpeechRecognizer sunta
  hai (WebView ka apna voice API bharosemand nahi hota, isliye Kotlin side
  se real recognition chalti hai)
- **Hand gesture control**: camera se real hand-tracking (MediaPipe Hands,
  on-device, sabse halka model taaki phone hang na ho). Gestures:
  - OPEN PALM → Home
  - FIST → Back
  - PEACE → Recent apps
  - POINT → Screen center tap
  - THUMBS UP / DOWN → Volume up / down
  - PINCH → zoom pulse (visual)
- **Touch**: hamesha normal chalta hai, kuch bhi lock/block nahi hota
- **Background voice**: System panel se ON karo, app minimize hone ke baad
  bhi "Jarvis" sunta rahega (permanent notification ke saath — Android ka
  rule hai, chhupa nahi sakte)

## Zaroori sach — dhyan se padho
1. **Hand gesture sirf real phone control tab karega jab Accessibility
   manually ON ho.** System panel me "Enable Touchless" button dabao,
   Settings khulegi, wahan JARVIS ko manually ON karo — ye Android ka
   security rule hai (jaise Google Voice Access me bhi hota hai), koi app
   khud-ba-khud ye permission nahi le sakta.
2. **Camera control sirf tab kaam karega jab app ka screen khula ho aur
   camera live dikh raha ho.** Phone lock/screen-off state me ya app band
   hone par camera se kuch detect nahi ho sakta — ye koi bug nahi, Android/
   phone hardware ki privacy-related hard limit hai, isko bypass karna
   possible nahi hai.
3. **Distance**: normal phone camera jaisa hi range — acchi roshni me
   ~1-2.5 meter tak reliably kaam karega. Bahut dur se ya andhere me
   detection weak ho jayega — ye webcam ki physical limitation hai.
4. **Pehli baar Hand Control kholne par internet chahiye** (MediaPipe ka
   ~10MB model download hota hai CDN se), uske baad browser cache karta hai.
5. **Phone hang nahi karega** — lightest hand-tracking model use kiya hai
   aur wo sirf tab chalta hai jab Hand Control panel khula ho, background
   me nahi chalta rehta. Background Voice ON karne par battery zyada use
   hogi (ye genuine hai, koi free trick nahi).
6. Kuch phones (Xiaomi/Vivo/Oppo) battery-saver me background service band
   kar dete hain — Settings me JARVIS ko "unrestricted battery" dena padega.

## GitHub se APK
1. Is zip ke andar ki saari files apne `jarvis-assistant` repo me push karo
   (poora `JARVIS-App` folder ka content root me daalna, ya folder ka naam
   jo bhi rakhna hai)
2. Actions tab me build automatically shuru hoga, ~3-5 min me APK ban jayega
3. "Artifacts" se `jarvis-debug-apk` download karo, phone me install karo
4. Mic + Camera permission allow karo
5. Voice/Hand icons use karo; System panel se background voice aur
   touchless (accessibility) ON karo

## Aage add kar sakte hain
- Zyada apps ka support (`MainActivity.kt` ke `appMap` me naye package
  names add karo)
- ElevenLabs ki deep AI voice (abhi Android ka built-in TTS hai)
- Zyada gestures (image me diye extra gestures jaise OK-sign, call-me,
  crossed-fingers — inko add karwana ho to bata dena, step-by-step add
  karte hain taaki reliability na gire)
