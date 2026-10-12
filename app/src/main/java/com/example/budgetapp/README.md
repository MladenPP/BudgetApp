# BudgetApp

Android aplikacija za praćenje troškova i štednje, napravljena u Kotlinu sa Jetpack Compose-om. Radi potpuno lokalno: nema prijave, naloga ni servera, svi podaci ostaju na telefonu.

Projekat za ispit iz predmeta Razvoj mobilnih aplikacija (RMA).

**Autor:** Mladen Petrovic, 132RN

## Ideja

Korisnik unese koliko prima i koliko mu odlazi na obaveze, a aplikacija mu pokazuje koliko može slobodno da troši i koliko treba da uštedi. Cilj štednje je procenat od onoga što ostane kad se od primanja oduzmu obaveze. Dok korisnik troši, to se odmah vidi na pregledu, a na kraju perioda dobija izveštaj o tome da li je uštedeo koliko je planirao.

## Šta aplikacija radi

- **Glavno primanje** (npr. plata) određuje budžetski period. Ako plata stiže 5. u mesecu, period ide od 5. do 5.
- **Stalna primanja** (npr. dodatni posao) ponavljaju se svakog meseca na zadati dan i ulaze u budžet kad taj dan stigne.
- **Bonusi** su jednokratna primanja. Dele se odmah po trenutnom procentu štednje (npr. 5.000 uz 20% daje 1.000 u štednju i 4.000 slobodno).
- **Stalni troškovi** (struja, voda, kirija) oduzimaju se od budžeta na početku perioda.
- **Progresivni troškovi** (hrana, gorivo): cela suma se rezerviše na početku. Pojedinačne kupovine troše tu rezervu i ne oduzimaju se ponovo. Tek ono preko rezerve skida se sa slobodnog novca.
- **Trenutni troškovi** (kafana, bioskop) skidaju se direktno sa slobodnog novca. Kad njega nestane, počinje da se troši štednja.
- **Izmena samo za jedan period:** iznos primanja ili troška može da se promeni za tekući period (npr. struja je ovog meseca 8.000), a sledeći period se vraća na standardnu vrednost.
- **Procenat štednje** korisnik podešava od 0 do 50%, podrazumevano je 20%.
- **Izveštaj** za svaki period: koliko je uštedeo, da li je više ili manje od plana, a ako je manje, gde je preterao (progresivni trošak koji je prešao rezervu, ili poslednji trenutni trošak). Završeni periodi se čuvaju i mogu da se pregledaju u istoriji.
- **Obaveštenje** sa izveštajem stiže poslednjeg dana perioda oko 18:00.
- **Resetovanje** u postavkama briše sve podatke i vraća aplikaciju na početno stanje.

## Kako se računa budžet

Za jedan period:

```
osnovica          = primanja - stalni troškovi - rezervacije za progresivne
cilj štednje      = osnovica * procenat štednje
slobodno (početak) = osnovica - cilj štednje
```

Primanja su glavno primanje i stalna primanja čiji je dan stigao. Slobodan novac se zatim smanjuje za trenutne troškove i za progresivne troškove preko rezerve, a povećava za deo bonusa koji nije otišao u štednju. Ako slobodnog novca nema dovoljno, razlika se skida sa štednje.

Ušteda u izveštaju je sve što ostane neutrošeno: štednja, slobodan novac i neiskorišćene rezerve progresivnih troškova. Poredi se sa planiranom štednjom (cilj plus deo bonusa koji je otišao u štednju).

## Arhitektura

Aplikacija je podeljena u slojeve, a podaci teku u jednom smeru: baza, repozitorijum, ViewModel, ekran.

```
ui          Compose ekrani i ViewModel-i (Pregled, Primanja, Troškovi, Izveštaj, Postavke)
data        Room baza, DataStore, repozitorijumi
domain      modeli i čista logika budžeta (bez Androida)
worker      WorkManager zadatak i obaveštenje
navigation  donja navigacija i NavHost
```

- **UI sloj.** Svaki ekran ima svoj ViewModel koji izlaže stanje kao `StateFlow`. Ekran samo prikazuje stanje i šalje akcije korisnika ViewModel-u.
- **Data sloj.** `BudgetRepository` je jedino mesto kroz koje UI dolazi do podataka. Čita iz Room-a i DataStore-a, računa stanje preko kalkulatora i vraća ga kao `Flow`, pa se ekrani sami osvežavaju kad se bilo šta u bazi promeni.
- **Domain sloj.** `BudgetCalculator`, `PeriodCalculator`, `ReportGenerator` i `SavingsRules` su obične Kotlin klase i funkcije bez ikakve zavisnosti od Androida. Zato se lako testiraju običnim jediničnim testovima, bez emulatora.
- **Zavisnosti** se prave ručno u `AppContainer`, a ViewModel-i ih dobijaju preko `AppViewModelProvider`.

## Tehnologije

| Šta | Čemu služi u aplikaciji |
|---|---|
| Kotlin | jezik |
| Jetpack Compose + Material 3 | ceo korisnički interfejs |
| Navigation Compose | donja navigacija između ekrana |
| ViewModel + StateFlow | stanje ekrana |
| Room | primanja, troškovi, bonusi, kupovine, izmene za mesece i sačuvani izveštaji |
| DataStore | procenat štednje i mala podešavanja |
| WorkManager | dnevna provera i obaveštenje o izveštaju |
| KSP | generisanje koda za Room |
| JUnit | jedinični testovi |

## Podaci

- Iznosi su celi dinari (`Long`), bez para, da ne bi bilo grešaka u zaokruživanju.
- Primanja i troškovi koji se ponavljaju čuvaju se kao šabloni. Stanje svakog perioda računa se iz šablona, izmena za taj period i unosa u njemu. Zato izmena "samo za ovaj mesec" ne pravi poseban zapis stavke.
- Izveštaj za završen period se sačuva kao snimak u posebnoj tabeli, da se kasnije ne menja kad korisnik izmeni primanja ili troškove.
- Baza je trenutno u verziji 2. Verzija 1 u 2 dodaje tabelu sa izveštajima i ima napisanu migraciju, pa postojeći podaci ostaju.

## Struktura projekta

```
app/src/main/java/com/example/budgetapp/
  MainActivity.kt, BudgetApplication.kt
  data/
    AppContainer.kt
    local/         BudgetDatabase, entity/, dao/
    mapper/        pretvaranje entiteta u modele i obrnuto
    repository/    BudgetRepository, CurrentSummary
    settings/      SettingsRepository (DataStore)
  domain/
    model/         Income, Expense, BudgetSummary, PeriodReport, ...
    calc/          BudgetCalculator, PeriodCalculator, ReportGenerator, SavingsRules
  navigation/      BudgetNavHost
  ui/
    BudgetApp.kt, AppViewModelProvider.kt
    components/    progress bar, dijalozi, kartice
    dashboard/ incomes/ expenses/ report/ settings/
    util/          formatiranje iznosa i datuma
    theme/
  worker/          ReportWorker, ReportScheduler, ReportNotifier
app/src/test/      jedinični testovi
```

## Testovi

Logika budžeta i izveštaja pokrivena je jediničnim testovima (41 test). Pokrivaju osnovni obračun, progresivne troškove, bonuse, izmene za jedan period, izveštaj, čuvanje izveštaja, formatiranje i granične slučajeve: glavno primanje na 29., 30. i 31. u mesecu, prestupnu godinu, prelaz godine, osnovicu koja je negativna i vrlo velike iznose. Jedan test proverava periode za svaki dan glavnog primanja (1 do 31) i svaki datum u dve godine.

Pokretanje: desni klik na `app/src/test` pa **Run Tests**, ili `./gradlew test`.

## Pokretanje aplikacije

Potrebno je Android Studio i uređaj ili emulator sa Androidom 8.0 (API 26) ili novijim.

1. Otvori projekat u Android Studiju i sačekaj Gradle sync.
2. Izaberi uređaj i pokreni aplikaciju (zeleno dugme Run).
3. Na Androidu 13 i novijem dozvoli obaveštenja kad aplikacija to zatraži.

Za proveru obaveštenja bez čekanja kraja perioda postoji dugme **Pošalji probno obaveštenje** u postavkama.

## Poznata ograničenja

- Podržana je samo jedna valuta (RSD) i samo uspravan položaj ekrana.
- Tekstovi su napisani na srpskom direktno u kodu, bez prevoda.
- Ako korisnik promeni dan glavnog primanja usred perioda, izmene za pojedinačne mesece vezane za stari period više se ne primenjuju.
- Brisanje stalne stavke briše je iz svih perioda, zajedno sa njenim izmenama i kupovinama. Već sačuvani izveštaji ostaju netaknuti.
- Obaveštenje stiže oko 18:00, ali tačno vreme može da se pomeri zbog uštede baterije na telefonu.

## Moguća proširenja

- Pregled potrošnje po kategorijama i grafikoni kroz više meseci
- Izvoz podataka
- Više jezika (prebacivanje tekstova u `strings.xml`)
- Rezervna kopija podataka