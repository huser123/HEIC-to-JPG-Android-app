# V1.0.0

A HEIC → JPG Android-alkalmazás első kiadása.

- Egy vagy több HEIC/HEIF kép átalakítása hagyományos JPG-vé.
- Teljesen offline működés, magyar kezelőfelület.
- Eredeti felbontás és helyes tájolás, 95-ös JPEG-kódolási minőség.
- Mentés a `Pictures/HEIC-JPG` mappába; az eredeti fájlok megmaradnak.
- Névütközéskor automatikus sorszámozás.
- Hibás kép esetén a köteg többi képe továbbra is feldolgozásra kerül.
- Nincs reklám, fiók, internetengedély vagy általános tárhelyengedély.

## Telepítés

Töltsd le a **HEIC-JPG-1.0.0.apk** fájlt. Android 10 vagy újabb rendszer szükséges;
a Xiaomi 11T Pro Android-verzióival kompatibilisre készült.

## Ellenőrzés

Sikeres fordítás és APK-aláírás-ellenőrzés. Mind a 6 Android-emulátoros
működési teszt sikeres, valódi HEIC-dekódolással és MediaStore-mentéssel.
Xiaomi 11T Pro készüléken is kipróbálva: működik.

Az APK mérete: **2 072 449 bájt**.

SHA-256:

```text
1de114fd1b40bbfaf33bbbc4a29e69febc19fe52c51c2f9f89dca1eb041973dd
```

A hagyományos JPG nem őrzi meg a HEIF kiegészítő képeit, mozgóképes adatait és
EXIF-metaadatait; az átalakított kép 8 bites sRGB. A privát aláírókulcs nem része
a nyilvános repónak.
