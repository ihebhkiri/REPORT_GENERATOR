package RHIS.com.RHIS.workforce.bootstrap;

import RHIS.com.RHIS.workforce.entity.AbsenceCongeEntity;
import RHIS.com.RHIS.workforce.entity.ContratEntity;
import RHIS.com.RHIS.workforce.entity.DetailEvenementEntity;
import RHIS.com.RHIS.workforce.entity.EmployeeEntity;
import RHIS.com.RHIS.workforce.entity.PointageEntity;
import RHIS.com.RHIS.workforce.entity.RestaurantEntity;
import RHIS.com.RHIS.workforce.entity.ShiftEntity;
import RHIS.com.RHIS.workforce.repository.AbsenceCongeRepository;
import RHIS.com.RHIS.workforce.repository.ContratRepository;
import RHIS.com.RHIS.workforce.repository.DetailEvenementRepository;
import RHIS.com.RHIS.workforce.repository.EmployeeRepository;
import RHIS.com.RHIS.workforce.repository.PointageRepository;
import RHIS.com.RHIS.workforce.repository.RestaurantRepository;
import RHIS.com.RHIS.workforce.repository.ShiftRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Order(100)
@Profile("!prod")
public class WorkforceDataSeeder implements ApplicationRunner {

    private static final int FIXTURE_COUNT = 50;

    private static final List<String> CITIES = List.of(
            "Paris", "Lyon", "Marseille", "Toulouse", "Bordeaux",
            "Lille", "Nantes", "Strasbourg", "Rennes", "Montpellier"
    );

    private static final List<String> STREETS = List.of(
            "de la Republique", "Victor Hugo", "Jean Jaures", "des Lilas", "du Commerce",
            "Pasteur", "de la Gare", "des Ecoles", "Nationale", "du Marche"
    );

    private static final List<String> LAST_NAMES = List.of(
            "Martin", "Bernard", "Thomas", "Petit", "Robert",
            "Richard", "Durand", "Dubois", "Moreau", "Laurent"
    );

    private static final List<String> FIRST_NAMES = List.of(
            "Emma", "Lucas", "Lea", "Hugo", "Chloe",
            "Louis", "Ines", "Gabriel", "Manon", "Arthur"
    );

    private static final List<String> ABSENCE_STATUSES = List.of(
            "EN_ATTENTE", "ACCEPTEE", "REFUSEE"
    );

    private final RestaurantRepository restaurantRepository;
    private final EmployeeRepository employeeRepository;
    private final ContratRepository contratRepository;
    private final ShiftRepository shiftRepository;
    private final PointageRepository pointageRepository;
    private final AbsenceCongeRepository absenceCongeRepository;
    private final DetailEvenementRepository detailEvenementRepository;

    public WorkforceDataSeeder(
            RestaurantRepository restaurantRepository,
            EmployeeRepository employeeRepository,
            ContratRepository contratRepository,
            ShiftRepository shiftRepository,
            PointageRepository pointageRepository,
            AbsenceCongeRepository absenceCongeRepository,
            DetailEvenementRepository detailEvenementRepository
    ) {
        this.restaurantRepository = restaurantRepository;
        this.employeeRepository = employeeRepository;
        this.contratRepository = contratRepository;
        this.shiftRepository = shiftRepository;
        this.pointageRepository = pointageRepository;
        this.absenceCongeRepository = absenceCongeRepository;
        this.detailEvenementRepository = detailEvenementRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<RestaurantEntity> restaurants = seedRestaurants();
        List<EmployeeEntity> employees = seedEmployees(restaurants);
        seedContrats(employees, restaurants);
        List<ShiftEntity> shifts = seedShifts(employees, restaurants);
        List<AbsenceCongeEntity> absences = seedAbsences(employees);
        seedPointages(employees, shifts, restaurants);
        seedDetails(absences, restaurants);

        verifyFixtureCounts();
        log.info("Initialisation RHIS terminee : {} lignes dans chacune des 7 tables metier.", FIXTURE_COUNT);
    }

    private List<RestaurantEntity> seedRestaurants() {
        List<RestaurantEntity> restaurants = new ArrayList<>(FIXTURE_COUNT);

        for (int index = 0; index < FIXTURE_COUNT; index++) {
            int number = index + 1;
            restaurants.add(new RestaurantEntity(
                    fixtureUuid("restaurant", number),
                    "Restaurant RHIS " + number,
                    "RST-%03d".formatted(number),
                    "%d rue %s, %s".formatted(
                            10 + number,
                            STREETS.get(index % STREETS.size()),
                            CITIES.get(index % CITIES.size())
                    ),
                    "PNT-%03d".formatted(number),
                    index % 2 == 0 ? "HEBDOMADAIRE" : "MENSUELLE"
            ));
        }

        return restaurantRepository.saveAllAndFlush(restaurants);
    }

    private List<EmployeeEntity> seedEmployees(List<RestaurantEntity> restaurants) {
        List<EmployeeEntity> employees = new ArrayList<>(FIXTURE_COUNT);

        for (int index = 0; index < FIXTURE_COUNT; index++) {
            int number = index + 1;
            LocalDate dateEntree = LocalDate.of(2022, 1, 3).plusDays(index * 11L);
            boolean actif = number % 10 != 0;

            employees.add(new EmployeeEntity(
                    fixtureUuid("employee", number),
                    "EMP-%04d".formatted(number),
                    LAST_NAMES.get(index % LAST_NAMES.size()),
                    FIRST_NAMES.get(index % FIRST_NAMES.size()),
                    dateEntree,
                    actif ? null : dateEntree.plusYears(2),
                    actif,
                    index % 4 == 0 ? 28.0f : 35.0f,
                    restaurants.get(index)
            ));
        }

        return employeeRepository.saveAllAndFlush(employees);
    }

    private void seedContrats(
            List<EmployeeEntity> employees,
            List<RestaurantEntity> restaurants
    ) {
        List<ContratEntity> contrats = new ArrayList<>(FIXTURE_COUNT);

        for (int index = 0; index < FIXTURE_COUNT; index++) {
            int number = index + 1;
            EmployeeEntity employee = employees.get(index);
            boolean tempsPartiel = index % 4 == 0;
            float hebdo = tempsPartiel ? 28.0f : 35.0f;
            float tauxHoraire = 12.5f + (index % 8) * 0.75f;

            contrats.add(new ContratEntity(
                    fixtureUuid("contrat", number),
                    hebdo,
                    tauxHoraire,
                    Math.round(tauxHoraire * hebdo * 52.0f / 12.0f * 100.0f) / 100.0f,
                    employee.getDateEntree().plusMonths(1),
                    employee.getDateSortie(),
                    employee.isStatut(),
                    tempsPartiel,
                    employee,
                    restaurants.get(index)
            ));
        }

        contratRepository.saveAllAndFlush(contrats);
    }

    private List<ShiftEntity> seedShifts(
            List<EmployeeEntity> employees,
            List<RestaurantEntity> restaurants
    ) {
        List<ShiftEntity> shifts = new ArrayList<>(FIXTURE_COUNT);

        for (int index = 0; index < FIXTURE_COUNT; index++) {
            int number = index + 1;
            LocalTime heureDebut = LocalTime.of(8 + index % 3, 0);

            shifts.add(new ShiftEntity(
                    fixtureUuid("shift", number),
                    LocalDate.of(2026, 1, 5).plusDays(index),
                    heureDebut,
                    heureDebut.plusHours(8),
                    480L,
                    index % 2 == 0,
                    index % 2 != 0,
                    index % 5 == 0,
                    employees.get(index),
                    restaurants.get(index)
            ));
        }

        return shiftRepository.saveAllAndFlush(shifts);
    }

    private List<AbsenceCongeEntity> seedAbsences(List<EmployeeEntity> employees) {
        List<AbsenceCongeEntity> absences = new ArrayList<>(FIXTURE_COUNT);

        for (int index = 0; index < FIXTURE_COUNT; index++) {
            int number = index + 1;
            LocalDate dateAbsence = LocalDate.of(2026, 4, 1).plusDays(index);

            absences.add(new AbsenceCongeEntity(
                    fixtureUuid("absence", number),
                    dateAbsence,
                    dateAbsence,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    8.0f,
                    true,
                    ABSENCE_STATUSES.get(index % ABSENCE_STATUSES.size()),
                    employees.get(index)
            ));
        }

        return absenceCongeRepository.saveAllAndFlush(absences);
    }

    private void seedPointages(
            List<EmployeeEntity> employees,
            List<ShiftEntity> shifts,
            List<RestaurantEntity> restaurants
    ) {
        List<PointageEntity> pointages = new ArrayList<>(FIXTURE_COUNT);

        for (int index = 0; index < FIXTURE_COUNT; index++) {
            int number = index + 1;
            ShiftEntity shift = shifts.get(index);

            pointages.add(new PointageEntity(
                    fixtureUuid("pointage", number),
                    shift.getDateJournee(),
                    shift.getHeureDebut(),
                    shift.getHeureFin(),
                    8.0f,
                    false,
                    employees.get(index),
                    shift,
                    restaurants.get(index)
            ));
        }

        pointageRepository.saveAllAndFlush(pointages);
    }

    private void seedDetails(
            List<AbsenceCongeEntity> absences,
            List<RestaurantEntity> restaurants
    ) {
        List<DetailEvenementEntity> details = new ArrayList<>(FIXTURE_COUNT);

        for (int index = 0; index < FIXTURE_COUNT; index++) {
            int number = index + 1;
            AbsenceCongeEntity absence = absences.get(index);

            details.add(new DetailEvenementEntity(
                    fixtureUuid("detail-evenement", number),
                    absence.getDateDebut(),
                    8.0f,
                    8.0f,
                    absence.getHeureDebut(),
                    absence.getHeureFin(),
                    absence,
                    restaurants.get(index)
            ));
        }

        detailEvenementRepository.saveAllAndFlush(details);
    }

    private UUID fixtureUuid(String entityName, int number) {
        String source = "rhis-demo:%s:%d".formatted(entityName, number);
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    private void verifyFixtureCounts() {
        verifyCount("rhis_restaurant", restaurantRepository.count());
        verifyCount("rhis_employee", employeeRepository.count());
        verifyCount("rhis_contrat", contratRepository.count());
        verifyCount("rhis_shift", shiftRepository.count());
        verifyCount("rhis_pointage", pointageRepository.count());
        verifyCount("rhis_absence_conge", absenceCongeRepository.count());
        verifyCount("rhis_detail_evenement", detailEvenementRepository.count());
    }

    private void verifyCount(String tableName, long actualCount) {
        if (actualCount != FIXTURE_COUNT) {
            throw new IllegalStateException(
                    "La table %s contient %d lignes au lieu de %d."
                            .formatted(tableName, actualCount, FIXTURE_COUNT)
            );
        }
    }
}
