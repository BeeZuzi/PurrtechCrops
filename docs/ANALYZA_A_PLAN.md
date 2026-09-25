# PurrTechCrops – analýza a plán vývoje

> Stav projektu: čistý Paper skeleton (Paper API `1.21.11`, Java 21, Gradle 9.4, `run-paper`), balíček `eu.purrtech.purrTechCrops`.

---

## 1. Cíl

**Right-click harvest:** hráč klikne pravým tlačítkem na **plně vyrostlou** plodinu →
1. plodina se „sklidí“ (vypadnou dropy jako při rozbití),
2. na stejném místě se **automaticky zasadí nová** (věk 0),
3. jako „cena“ za zasazení se z dropů odečte 1 semínko / plod.

Na nevyrostlou plodinu se nic nemění – funguje vanilla chování (např. kostní moučka).

---

## 2. Funkční požadavky

| ID | Požadavek | Priorita |
|----|-----------|----------|
| F1 | Pravý klik na zralou plodinu ji sklidí a přesadí | MUST |
| F2 | Podporované plodiny: pšenice, mrkev, brambory, řepa | MUST |
| F3 | Z dropů se odečte 1 kus „sadby“ (seeds / carrot / potato / beetroot_seeds) | MUST |
| F4 | Respektovat ochranu území (WorldGuard, GriefPrevention, Lands…) | MUST |
| F5 | Permission `purrtechcrops.use` (default: true) | MUST |
| F6 | Nether wart a kakao (zachovat natočení kakaa) | SHOULD |
| F7 | Konfigurace (`config.yml`) + `/ptc reload` | SHOULD |
| F8 | Efekty: zvuk, částice, animace máchnutí rukou | SHOULD |
| F9 | Fortune z motyky v ruce + volitelné opotřebení motyky | COULD |
| F10 | Režim dropu: na zem / přímo do inventáře (přebytek na zem) | COULD |
| F11 | `/ptc toggle` – hráč si funkci vypne/zapne (uloženo v PDC hráče) | COULD |
| F12 | Vlastní event `CropHarvestEvent` (API pro jiné pluginy) | COULD |
| F13 | Integrace CoreProtect (logování), Jobs/mcMMO (odměny) | LATER |
| F14 | Integrace Residence (flag `harvest`) | MUST (hotovo) |

**Mimo scope (zatím):** melouny/dýně (stonky), cukrová třtina, kaktus, pitcher plant (2 bloky vysoký), torchflower (po dozrání se mění na jiný blok), sladké bobule (vanilla už right-click sklizeň má).

---

## 3. Technická analýza

### 3.1 Detekce kliknutí
- Event: `PlayerInteractEvent`, `Action.RIGHT_CLICK_BLOCK`.
- **Pozor – event se volá 2× (hlavní i vedlejší ruka).** Zpracovávat jen `event.getHand() == EquipmentSlot.HAND`, jinak hrozí dvojí sklizeň / duplikace.
- `@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)` a ručně kontrolovat `event.useInteractedBlock() == Event.Result.DENY` → pokud ochranný plugin interakci zakázal, nic neděláme. (`isCancelled()` u tohoto eventu není spolehlivé, protože u kliku do vzduchu je event předcancelovaný.)
- Priorita HIGH = ochranné pluginy (běží typicky na LOW/NORMAL) rozhodnou dřív než my.

### 3.2 Je plodina zralá?
```java
if (block.getBlockData() instanceof Ageable ageable
        && ageable.getAge() == ageable.getMaximumAge()) { ... }
```
Nepoužívat natvrdo čísla věku – pšenice má max 7, řepa 3, nether wart 3, kakao 2.

### 3.3 Definice plodin (registry)
| Blok | Sadba (odečítá se) | Max věk | Poznámka |
|------|--------------------|---------|----------|
| `WHEAT` | `WHEAT_SEEDS` | 7 | |
| `CARROTS` | `CARROT` | 7 | |
| `POTATOES` | `POTATO` | 7 | jedovaté brambory normálně padají |
| `BEETROOTS` | `BEETROOT_SEEDS` | 3 | |
| `NETHER_WART` | `NETHER_WART` | 3 | na soul sand |
| `COCOA` | `COCOA_BEANS` | 2 | `Directional` – přes `clone()` BlockData se natočení zachová |

### 3.4 Dropy
- `block.getDrops(toolInHand, player)` – vrací dropy podle loot tabulky **včetně Fortune** a datapacků.
- Z výsledku odečíst 1 ks sadby. Vanilla loot tabulky u zralých plodin vždy vrací ≥ 1 kus sadby, ale datapack to může změnit → **fallback** (konfigurovatelné):
  - `FREE` – zasadit i tak (default),
  - `INVENTORY` – vzít sadbu z inventáře hráče,
  - `SKIP` – nezasazovat, jen sklidit (blok se nastaví na AIR).
- Pokud `apply-fortune: false`, volat `getDrops()` s prázdným nástrojem.

### 3.5 Přesazení
```java
Ageable newData = (Ageable) ageable.clone();
newData.setAge(0);
block.setBlockData(newData, true);
```
Klon zachová ostatní vlastnosti (natočení kakaa). Neměnit blok na AIR a zpět → žádné problémy s fyzikou / farmland.

### 3.6 Zabránění vanilla akci
Po úspěšné sklizni `event.setCancelled(true)` (resp. `setUseItemInHand(DENY)` + `setUseInteractedBlock(DENY)`), aby se nepoužil předmět v ruce (např. položení bloku, použití kostní moučky).

### 3.7 Ochrana území a kompatibilita
Dvě úrovně (konfigurovatelné):
1. **Default:** spolehnout se na `useInteractedBlock() == DENY` z `PlayerInteractEvent`.
2. **`strict-protection: true`:** před sklizní vyvolat syntetický `BlockBreakEvent`; když ho někdo zruší, nesklízet. Výhoda: funguje se všemi ochranami a s Jobs/mcMMO/CoreProtect. Nevýhoda: některé pluginy mohou na BlockBreakEvent reagovat vedlejšími efekty (dvojité dropy mcMMO) → proto volitelné, default `false`.

Navíc vlastní `CropHarvestEvent` (cancellable, nese hráče, blok, list dropů – měnitelný) → ostatní naše pluginy (PurrTech ekosystém) se mohou napojit.

### 3.8 Herní režimy a omezení
- Spectator → ignorovat. Adventure → ignorovat (nemůže rozbíjet bloky), volitelně povolit.
- Creative → sklízet, ale dropy volitelně nepouštět (`creative-drops: false`).
- Volitelně `require-hoe: true` – funguje jen s motykou v ruce.
- Volitelně `disable-when-sneaking: true` – při plížení vanilla chování.
- Blacklist světů (`disabled-worlds`).

### 3.9 Výkon a vlákna
- Vše běží synchronně v handleru eventu, O(1), žádné asynchronní operace ani plánovače → zanedbatelná zátěž.
- Folia: logika je v rámci regionu kliknutého bloku, takže by fungovala; `folia-supported: true` deklarovat až po otestování (LATER).

---

## 4. Architektura

```
eu.purrtech.purrTechCrops
├── PurrTechCrops.java              // main: načtení configu, registrace listeneru a příkazů
├── config/
│   └── PluginConfig.java           // immutable record s hodnotami z config.yml, reload = nová instance
├── crop/
│   ├── CropDefinition.java         // record(Material block, Material replantItem)
│   └── CropRegistry.java           // Map<Material, CropDefinition>, plněno z configu
├── harvest/
│   ├── HarvestService.java         // canHarvest(...), harvest(...) – čistá doménová logika
│   ├── ReplantFallback.java        // enum FREE / INVENTORY / SKIP
│   └── DropMode.java               // enum GROUND / INVENTORY
├── listener/
│   └── CropInteractListener.java   // tenká vrstva: filtr eventu → HarvestService
├── api/event/
│   └── CropHarvestEvent.java       // vlastní cancellable event
├── command/
│   └── PtcCommand.java             // Brigadier přes Paper LifecycleEvents.COMMANDS (reload, toggle)
└── util/
    └── Effects.java                // zvuk, částice, swing
```

**Zásady:** listener neobsahuje logiku (jen filtruje a deleguje), `HarvestService` je testovatelný bez serveru co nejvíc, config je neměnný objekt vyměněný při reloadu (žádné čtení `getConfig()` v hot-path).

### Tok zpracování
```
PlayerInteractEvent
  → hand == HAND && action == RIGHT_CLICK_BLOCK?
  → useInteractedBlock != DENY?
  → svět povolen, gamemode OK, permission, toggle, sneak, hoe?
  → blok v CropRegistry && Ageable && age == max?
  → [strict] BlockBreakEvent nezrušen?
  → spočítat dropy (getDrops) − 1 sadba (+ fallback)
  → CropHarvestEvent nezrušen?
  → přesadit (age 0) → vydat dropy → efekty → opotřebit motyku → cancel původního eventu
```

---

## 5. Konfigurace (návrh `config.yml`)

```yaml
# Plodiny, na které right-click harvest funguje
crops:
  WHEAT: WHEAT_SEEDS
  CARROTS: CARROT
  POTATOES: POTATO
  BEETROOTS: BEETROOT_SEEDS
  NETHER_WART: NETHER_WART
  COCOA: COCOA_BEANS

disabled-worlds: []

harvest:
  require-hoe: false
  disable-when-sneaking: false
  apply-fortune: true
  damage-hoe: true            # jen pokud drží motyku
  drop-mode: GROUND           # GROUND | INVENTORY
  replant-fallback: FREE      # FREE | INVENTORY | SKIP
  creative-drops: false
  allow-adventure: false

protection:
  strict: false               # vyvolat BlockBreakEvent pro kontrolu ochran

effects:
  swing-hand: true
  sound: BLOCK_CROP_BREAK
  particles: true

messages:                     # MiniMessage formát
  prefix: "<gradient:#ff9ecd:#a78bfa>PurrTechCrops</gradient> <gray>»</gray> "
  reloaded: "<green>Konfigurace načtena.</green>"
  toggled-on: "<green>Sklízení pravým klikem zapnuto.</green>"
  toggled-off: "<red>Sklízení pravým klikem vypnuto.</red>"
  no-permission: "<red>Na tohle nemáš oprávnění.</red>"
```

Neplatné názvy materiálů při načtení zalogovat jako warning a přeskočit (plugin nespadne).

## 6. Oprávnění a příkazy

| Příkaz | Oprávnění | Default |
|--------|-----------|---------|
| (sklízení) | `purrtechcrops.use` | true |
| `/ptc toggle` | `purrtechcrops.toggle` | true |
| `/ptc reload` | `purrtechcrops.admin` | op |

---

## 7. Plán implementace (fáze)

### Fáze 0 – Příprava (≈0,5 h)
- [x] `git init`, `.gitignore` (build, .gradle, .idea, run)
- [x] Doplnit `plugin.yml` (description, author, permissions)
- [x] Ověřit `./gradlew build` (Gradle wrapper 9.7.1 – run-paper 3.1.0 vyžaduje 9.7+, JDK 21 přes foojay toolchain resolver)
- [ ] Ověřit `./gradlew runServer` (ručně)

### Fáze 1 – MVP jádro (≈2 h)
- [x] `CropDefinition` + `CropRegistry` (natvrdo 4 základní plodiny)
- [x] `CropInteractListener` s filtrem ruky, akce, `useInteractedBlock`
- [x] `HarvestService`: kontrola zralosti, `getDrops`, odečet sadby, přesazení, drop na zem
- [x] Cancel původního eventu
- **Hotovo když:** na testovacím serveru lze sklidit pšenici/mrkev/brambory/řepu, nic se neduplikuje, nezralé plodiny se nechovají jinak.

### Fáze 2 – Konfigurace a další plodiny (≈2 h)
- [x] `config.yml` + `PluginConfig` (record), validace materiálů
- [x] Nether wart, kakao (ověřit natočení)
- [x] Fallback strategie, drop-mode INVENTORY, Fortune, opotřebení motyky
- [x] Gamemode / sneak / require-hoe / disabled-worlds

### Fáze 3 – Ochrana a API (≈1,5 h)
- [x] `strict` režim přes syntetický `BlockBreakEvent`
- [x] `CropHarvestEvent`
- [ ] Test s WorldGuard regionem (klik v cizím regionu nesmí nic udělat) – ručně

### Fáze 4 – UX (≈1,5 h)
- [x] Efekty (zvuk, částice, swing) – vanilla efekt rozbití bloku (`Effect.STEP_SOUND`)
- [x] Brigadier příkazy `/ptc reload`, `/ptc toggle` (toggle v PDC hráče)
- [x] MiniMessage zprávy (default anglicky, tag `<prefix>`)

### Fáze 5 – Testy a release (≈2 h)
- [x] Unit testy (MockBukkit 4.116.3) – 35 testů, viz níže
- [x] Manuální testovací checklist → [TESTOVANI.md](TESTOVANI.md) (sepsaný, zatím neprovedený)
- [x] Detekce konfliktních pluginů při startu (warning pro mcMMO/AuraSkills/Jobs, jen při `protection.strict`)
- [x] `config-version` + doplňování nových klíčů, jazyky `lang/en.yml` + `lang/cs.yml` (klíč `language`)
- [x] Verze `1.0.0`, README (anglicky), build jar
- [ ] Provést ruční testy, stránka na Modrinth/Hangar

### Fáze 6 – Integrace Residence
- [x] Residence sám hlídá flag `harvest` jen u sladkých bobulí a jeskynních lián → bez hooku by šly sklízet plodiny v cizích rezidencích
- [x] Rozhraní `HarvestGuard` pro ochranné pluginy, `ResidenceGuard` kopíruje kontrolu Residence (globální vypnutí flagu, ResAdmin, `FlagPermissions.has(..., harvest, true)`)
- [x] `softdepend: [Residence]`, závislost `compileOnly` z JitPacku (bez tranzitivních závislostí), hook se použije jen při zapnutém Residence
- [x] `hooks.residence` v configu (config-version 2), zpráva `harvest-denied` v action baru
- [x] Testy: zákaz ochrany zastaví sklizeň ještě před jakýmkoli eventem; hlavní třída se načte bez Residence
- [x] Verze `1.1.0`
- [ ] Ruční test s Residence (viz TESTOVANI.md)

**Odhad celkem:** ~10 h čisté práce + ~1,5 h Residence.

---

## 8. Testování

**Automatické (MockBukkit + JUnit 5):**
- zralá pšenice → age 0, dropy obsahují wheat a o 1 méně seeds
- nezralá plodina → beze změny, event nezrušen
- off-hand event → ignorován
- `useInteractedBlock = DENY` → beze změny
- bez permission → beze změny
- kakao → natočení zachováno
- zrušený `CropHarvestEvent` → beze změny

**Manuální checklist (runServer):**
- všechny plodiny, Fortune III motyka, motyka se opotřebí a může se rozbít
- spam kliknutí / držení pravého tlačítka → žádná duplikace
- creative / adventure / spectator
- plný inventář při `drop-mode: INVENTORY` → přebytek padá na zem
- WorldGuard region bez práv
- `/ptc reload` se špatným materiálem v configu → warning, plugin běží

---

## 9. Rizika

| Riziko | Dopad | Mitigace |
|--------|-------|----------|
| Dvojité volání eventu (2 ruce) | duplikace itemů | filtr `EquipmentSlot.HAND` + test |
| Obejití ochrany území | griefing | priorita HIGH + `useInteractedBlock`, volitelně strict režim |
| Kolize s jiným harvest pluginem (např. v mcMMO, AuraSkills) | dvojí sklizeň | detekce při startu + warning v konzoli, dokumentace v README |
| Neznámé prostředí serverů (veřejný plugin) | špatné recenze, bug reporty | bezpečné defaulty, validace configu, `/ptc debug` výpis prostředí (LATER) |
| Datapack změní loot tabulky | chybí sadba | fallback strategie |
| Změny API mezi verzemi MC | nekompatibilita | jen `Ageable`/`getDrops`, žádné NMS |

---

## 10. Rozhodnutí

| Otázka | Rozhodnutí |
|--------|-----------|
| Cílová verze | **1.21.11** (v1.0). Další verze až po dokončení – viz kap. 11. |
| Pro koho | **Veřejný plugin pro libovolné servery** → nevíme, jaké ochrany/jobs pluginy tam běží, vše musí být obecné a konfigurovatelné. |
| Dropy | Konfigurovatelné, default `GROUND` (nejblíž vanilla). |
| Motyka | Default funguje i s prázdnou rukou, `require-hoe` volitelně. |
| Folia | Až po v1.0 (LATER). |
| Melouny/dýně/třtina | Mimo v1.0. |

### Důsledky „veřejného pluginu“ pro návrh
- **Ochrana území:** nesmíme spoléhat na konkrétní plugin. Default `useInteractedBlock` kontrola + `protection.strict` přepínač (syntetický `BlockBreakEvent`) zdokumentovaný v README pro servery s neobvyklými ochranami.
- **Konflikty s jinými harvest funkcemi:** při startu detekovat známé pluginy s podobnou funkcí (mcMMO, AuraSkills, jiné replant pluginy) a vypsat **warning do konzole** s doporučením. Soft-depend jen v `plugin.yml` (`softdepend`), žádná tvrdá závislost.
- **Robustní config:** neplatné hodnoty = warning + default, nikdy pád. Automatické doplnění nových klíčů do existujícího configu při updatu (`config-version` + `copyDefaults`).
- **Zprávy/lokalizace:** všechny texty v configu (MiniMessage), default anglicky + `messages_cs.yml`.
- **Žádný NMS / reflection** – jen Paper API, aby šlo snadno přidat další verze.
- **Distribuce:** Modrinth + Hangar, bStats (volitelně vypnutelné), update checker (LATER).

---

## 11. Strategie pro další verze MC (po v1.0)

1. Kód píšeme jen proti stabilnímu API (`Ageable`, `Block#getDrops`, `PlayerInteractEvent`, `Directional`) – ta existují beze změny napříč 1.20–1.21.x.
2. Před přidáním verze ověřit: kompiluje se proti starší `paper-api` bez chyb? Pokud ano, stačí snížit `api-version` v `plugin.yml` na nejnižší podporovanou (např. `1.20`) a vydat **jeden jar pro více verzí**.
3. Pokud se API rozejde (materiály, Brigadier/lifecycle příkazy existují až od 1.20.6), oddělit verzově závislé části za rozhraní (`VersionAdapter`) – multi-modul Gradle jen v krajním případě.
4. CI matice: build + MockBukkit testy proti každé podporované verzi; `run-paper` ruční smoke test.
