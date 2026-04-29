package com.desapp.futbolplayerstokens.modelo;

public enum TeamEnum {

    ATHLETIC_CLUB(77, "Athletic Club"),
    ATLETICO_MADRID(78, "Atletico"),
    OSASUNA(79, "Osasuna"),
    ESPANYOL(80, "Espanyol"),
    BARCELONA(81, "Barcelona"),
    GETAFE(82, "Getafe"),
    REAL_MADRID(86, "Real Madrid"),
    RAYO_VALLECANO(87, "Rayo Vallecano"),
    LEVANTE(88, "Levante"),
    MALLORCA(89, "Mallorca"),
    REAL_BETIS(90, "Real Betis"),
    REAL_SOCIEDAD(92, "Real Sociedad"),
    VILLARREAL(94, "Villarreal"),
    VALENCIA(95, "Valencia"),
    ALAVES(263, "Deportivo Alaves"),
    ELCHE(285, "Elche"),
    GIRONA(298, "Girona"),
    CELTA_VIGO(558, "Celta Vigo"),
    SEVILLA(559, "Sevilla"),
    REAL_OVIEDO(1048, "Real Oviedo"),

    // Premier League
    ARSENAL(57, "Arsenal"),
    ASTON_VILLA(58, "Aston Villa"),
    CHELSEA(61, "Chelsea"),
    EVERTON(62, "Everton"),
    FULHAM(63, "Fulham"),
    LIVERPOOL(64, "Liverpool"),
    MANCHESTER_CITY(65, "Man City"),
    MANCHESTER_UNITED(66, "Man Utd"),
    NEWCASTLE(67, "Newcastle"),
    SUNDERLAND(71, "Sunderland"),
    TOTTENHAM(73, "Tottenham"),
    WOLVES(76, "Wolves"),
    BURNLEY(328, "Burnley"),
    LEEDS(341, "Leeds"),
    NOTTINGHAM_FOREST(351, "Nottingham Forest"),
    CRYSTAL_PALACE(354, "Crystal Palace"),
    BRIGHTON(397, "Brighton"),
    BRENTFORD(402, "Brentford"),
    WEST_HAM(563, "West Ham"),
    BOURNEMOUTH(1044, "Bournemouth"),

    // Serie A
    MILAN(98, "AC Milan"),
    FIORENTINA(99, "Fiorentina"),
    ROMA(100, "Roma"),
    ATALANTA(102, "Atalanta"),
    BOLOGNA(103, "Bologna"),
    CAGLIARI(104, "Cagliari"),
    GENOA(107, "Genoa"),
    INTER(108, "Inter"),
    JUVENTUS(109, "Juventus"),
    LAZIO(110, "Lazio"),
    PARMA(112, "Parma Calcio 1913"),
    NAPOLI(113, "Napoli"),
    UDINESE(115, "Udinese"),
    VERONA(450, "Verona"),
    CREMONESE(457, "Cremonese"),
    SASSUOLO(471, "Sassuolo"),
    PISA(487, "Pisa"),
    TORINO(586, "Torino"),
    LECCE(5890, "Lecce"),
    COMO(7397, "Como"),

    // Bundesliga
    KOLN(1, "FC Koln"),
    HOFFENHEIM(2, "Hoffenheim"),
    LEVERKUSEN(3, "Leverkusen"),
    DORTMUND(4, "Borussia Dortmund"),
    BAYERN(5, "Bayern"),
    HAMBURG(7, "Hamburg"),
    STUTTGART(10, "Stuttgart"),
    WOLFSBURG(11, "Wolfsburg"),
    WERDER_BREMEN(12, "Werder Bremen"),
    MAINZ(15, "Mainz"),
    AUGSBURG(16, "Augsburg"),
    FREIBURG(17, "Freiburg"),
    MONCHENGLADBACH(18, "Borussia M.Gladbach"),
    FRANKFURT(19, "Eintracht Frankfurt"),
    ST_PAULI(20, "St. Pauli"),
    UNION_BERLIN(28, "Union Berlin"),
    HEIDENHEIM(44, "FC Heidenheim"),
    RB_LEIPZIG(721, "RBL"),

    // Ligue 1
    TOULOUSE(511, "Toulouse"),
    BREST(512, "Brest"),
    MARSEILLE(516, "Marseille"),
    AUXERRE(519, "Auxerre"),
    LILLE(521, "Lille"),
    NICE(522, "Nice"),
    LYON(523, "Lyon"),
    PSG(524, "PSG"),
    LORIENT(525, "Lorient"),
    RENNES(529, "Rennes"),
    ANGERS(532, "Angers"),
    LE_HAVRE(533, "Le Havre"),
    NANTES(543, "Nantes"),
    METZ(545, "Metz"),
    LENS(546, "Lens"),
    MONACO(548, "Monaco"),
    STRASBOURG(576, "Strasbourg"),
    PARIS_FC(1045, "Paris FC");

    private final int id;
    private final String name;

    TeamEnum(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static TeamEnum fromId(int id) {
        for (TeamEnum team : values()) {
            if (team.id == id) {
                return team;
            }
        }
        throw new IllegalArgumentException("No team with id " + id);
    }
}
