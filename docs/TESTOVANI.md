# Ruční testování před vydáním

Automatické testy (`./gradlew test`, MockBukkit) pokrývají logiku sklizně, config a API.
Tento checklist ověřuje to, co mock server neumí: skutečné loot tabulky, klienta, efekty, příkazy a jiné pluginy.

Server: `./gradlew runServer` (při prvním spuštění potvrdit EULA v `run/eula.txt`).

## Základ
- [ ] Pšenice, mrkev, brambory, řepa, nether wart: zralá → dropy o 1 sadbu méně, plodina věk 0
- [ ] Kakao na všech 4 stranách kmene → natočení zůstane
- [ ] Nezralá plodina → nic, kostní moučka funguje
- [ ] Předmět ve vedlejší ruce, držení pravého tlačítka, rychlé klikání → žádná duplikace
- [ ] Motyka Fortune III → víc dropů
- [ ] Motyka se opotřebí, Unbreaking funguje, skoro zničená motyka se rozbije

## Režimy a nastavení
- [ ] Creative → přesadí bez dropů; spectator/adventure → nic
- [ ] `drop-mode: INVENTORY` s plným inventářem → přebytek na zem
- [ ] `require-hoe`, `disable-when-sneaking`, `disabled-worlds`
- [ ] Nesmysl v configu (`crops: {STONE: DIRT}`, `drop-mode: blbost`) → varování v konzoli, plugin běží

## Příkazy a zprávy
- [ ] `/ptc toggle` → vypnuto i po odpojení/restartu, znovu `/ptc toggle` → zapnuto
- [ ] `/ptc reload` → změny v configu platí hned, zpráva s počtem plodin
- [ ] Hráč bez op nevidí `reload` v tab-complete
- [ ] `/ptc toggle` z konzole → „jen pro hráče“
- [ ] `language: cs` + `/ptc reload` → české zprávy; `language: xx` → varování, angličtina

## Aktualizace configu
- [ ] Smazat z `config.yml` sekci `effects` a jeden řádek z `crops` → po restartu je `effects` zpět, smazaná plodina ne

## Ochrana a kompatibilita
- [ ] WorldGuard region bez práv → nic se nestane
- [ ] Totéž s `protection.strict: true`
- [ ] `protection.strict: true` s mcMMO / Jobs → varování v konzoli při startu
