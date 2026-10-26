# HEIC → JPG Android-alkalmazás

Egyszerű, magyar nyelvű, teljesen helyben működő képátalakító. Android 10 vagy
újabb rendszerre, így a Xiaomi 11T Pro telefonra is készült.

## Letöltés – V1.0.0

**[Telepíthető APK letöltése](https://github.com/huser123/HEIC-to-JPG-Android-app/releases/download/V1.0.0/HEIC-JPG-1.0.0.apk)** · [V1.0.0 kiadás](https://github.com/huser123/HEIC-to-JPG-Android-app/releases/tag/V1.0.0) · [V1.0.0 forrásállapot](https://github.com/huser123/HEIC-to-JPG-Android-app/tree/V1.0.0)

Az aláírt, önálló APK Android 10 vagy újabb rendszeren telepíthető. Ugyanez a
fájl a [`RELEASE/V1.0.0` mappában](RELEASE/V1.0.0) is megtalálható:
[APK letöltése a repóból](RELEASE/V1.0.0/HEIC-JPG-1.0.0.apk?raw=true).

- Verzió: **1.0.0** (`versionCode = 1`)
- Méret: **2 072 449 bájt**
- Ellenőrzés: sikeres fordítás, APK-aláírás-ellenőrzés és 6 sikeres,
  valódi HEIC-képekkel futtatott Android-emulátoros teszt.
- Fizikai Xiaomi-készüléken még nem volt kipróbálva.

SHA-256:

```text
1de114fd1b40bbfaf33bbbc4a29e69febc19fe52c51c2f9f89dca1eb041973dd
```

## Használat

1. Töltsd le a fenti **V1.0.0 APK-t**, és telepítsd a telefonon.
   Ha az Android kéri, engedélyezd a telepítést annak a böngészőnek vagy
   fájlkezelőnek, amellyel megnyitod az APK-t.
2. Indítsd el a **HEIC → JPG** alkalmazást.
3. Koppints a **Képek kiválasztása** gombra. Több fájl kijelöléséhez a
   rendszer fájlválasztójában általában hosszan kell megérinteni az első képet.
4. Koppints a **Konvertálás** gombra. A feldolgozás végéig hagyd nyitva az appot.
5. A JPG-k a belső tárhely **Pictures/HEIC-JPG** mappájába kerülnek, és a
   galériában is megjelennek. Magyar fájlkezelő ezt a mappát „Képek” néven is
   mutathatja.

Az eredeti fájlok megmaradnak. Névütközéskor `_1`, `_2` stb. kerül a névbe.
Minden fájl külön készül el; egy hibás kép nem akadályozza a többi mentését.
A feldolgozás közben a kijelző bekapcsolva marad. Képernyőelforgatáskor a
folyamat folytatódik. Kényszerített leállítás vagy a folyamat rendszer általi
megszüntetése után nincs automatikus háttérben folytatás.

## Képformátum és adatkezelés

- Bemenet: Android által dekódolható HEIC/HEIF állókép, a fájl valódi
  formátumának ellenőrzésével. A fájlválasztó más képeket is megmutathat;
  ezekre az app hibát jelez.
- Kimenet: hagyományos, 8 bites sRGB JPEG, 95-ös kódolási minőség, eredeti
  pixelszám. A JPEG tömörítés veszteséges.
- Az Android ImageDecoder kezeli a forgatást és tükrözést; átlátszó részek
  fehér hátteret kapnak. HDR/tág színtér esetén hagyományos sRGB JPG készül.
- Többképes HEIF esetén az elsődleges állókép kerül exportálásra; mozgóképet,
  mélységtérképet és egyéb kiegészítő adatot nem exportál.
- Az EXIF-adatok (például GPS, kameramodell, eredeti készítési idő) nem
  kerülnek a JPG-be. A tájolás a képpontokban helyes, nem EXIF-jelzővel.
- Nincs internetengedély, reklám, analitika, fiók vagy szerveroldali konvertálás.
- Nincs általános tárhely- vagy teljes galériahozzáférés: a rendszer
  fájlválasztója ad olvasási jogot a kijelölt fájlokra. Új kiválasztáskor az app
  elengedi a korábbi kijelölés tartós olvasási engedélyeit.
- Ha felhős fájlszolgáltatóból választasz, annak az alkalmazásnak szüksége lehet
  internetre a bemenet letöltéséhez; maga a konverter nem használ hálózatot.

## A forrás megnyitása és fordítása

Nyisd meg ezt a mappát Android Studióban. Szükséges:

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.0.0
- Gradle 8.10.2 (a mellékelt wrapper automatikusan letölti)

Az SDK elérési útját állítsd be Android Studióban vagy a saját
`local.properties` fájlodban: `sdk.dir=/sajat/android/sdk`.

Linux/macOS:

```sh
./gradlew assembleRelease
```

Windows:

```bat
gradlew.bat assembleRelease
```

Ha beállítottad az aláírást az alábbiak szerint, a telepíthető kimenet:
`app/build/outputs/apk/release/app-release.apk`.
Aláírási beállítás nélkül a release kimenet `app-release-unsigned.apk`, amely
közvetlenül nem telepíthető.

Gyors kipróbáláshoz aláírási beállítás nélkül is készül automatikusan aláírt
debug APK: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
Kimenete: `app/build/outputs/apk/debug/app-debug.apk`.

Az első fordítás internetet igényel az Android- és Kotlin-függőségek
letöltéséhez. Az alkalmazás futása nem igényel internetet.

## Aláírás és későbbi frissítés

A privát aláírókulcs és a jelszófájl nem része a nyilvános repónak.

1. Másold a `signing/release.properties.example` fájlt
   `signing/release.properties` néven, és töltsd ki a saját értékeiddel.
2. Helyezd el a kulcsot a konfigurációban megadott útvonalon. Az útvonal a
   projekt gyökeréhez képest értendő.
3. Futtasd a fenti `assembleRelease` parancsot.

Új, saját kulcsot a JDK `keytool` eszközével hozhatsz létre. A parancs
interaktívan kéri a jelszót és a tanúsítvány adatait:

```sh
keytool -genkeypair -keystore signing/heic-jpg.jks -alias heic-jpg -keyalg RSA -keysize 3072 -validity 10000
```

A korábban telepített alkalmazás frissítéséhez az eredeti aláírókulcs és
nagyobb `versionCode` szükséges. Az eredeti, privát forráscsomag tulajdonosa
abból állíthatja vissza a `signing/heic-jpg.jks` és
`signing/release.properties` fájlokat. Más kulccsal készített APK nem
telepíthető közvetlen frissítésként a korábbi verzióra.

A `.gitignore` kizárja az aláírókulcsokat és a valódi jelszófájlt; ezeket
őrizd meg biztonságos, privát helyen.

## Tesztek

Az `app/src/androidTest` valódi HEIC-próbaképekkel, Android-eszközön vagy
emulátoron futó teszteket tartalmaz. Nem helyettesítik saját függvényekkel a
dekódert és a MediaStore-ot.

```sh
./gradlew connectedDebugAndroidTest
./gradlew lint
```

Ellenőrzik a JPG-aláírást, pixelszámot, színeket, 90 fokos elforgatást,
az eredeti változatlanságát, a publikált MediaStore-bejegyzést, a névütközést,
a hibás/eltérő formátumú bemenet elutasítását és a kötegelt feldolgozás
folytatását egy sérült kép után.

## Szerkezet

- `MainActivity.kt`: magyar, egylapos Android-felület és rendszer-fájlválasztó.
- `Converter.kt`: HEIF-dekódolás, JPEG-kódolás, biztonságos MediaStore-mentés.
- `ConverterApplication.kt`: soros feldolgozás és elforgatást túlélő állapot.

A futó alkalmazásnak a Kotlin standard könyvtáron kívül nincs külső
futásidejű függősége. Android 10 a minimum; az alkalmazás nem Play Áruházból
települ, közvetlen APK-telepítésre készült.
