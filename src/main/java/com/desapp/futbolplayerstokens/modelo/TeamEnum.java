package com.desapp.futbolplayerstokens.modelo;

public enum TeamEnum {

    ATHLETIC_CLUB(77, "Athletic Club", "LaLiga"),
    ATLETICO_MADRID(78, "Atletico", "LaLiga"),
    OSASUNA(79, "Osasuna", "LaLiga"),
    ESPANYOL(80, "Espanyol", "LaLiga"),
    BARCELONA(81, "Barcelona", "LaLiga"),
    GETAFE(82, "Getafe", "LaLiga"),
    REAL_MADRID(86, "Real Madrid", "LaLiga"),
    RAYO_VALLECANO(87, "Rayo Vallecano", "LaLiga"),
    LEVANTE(88, "Levante", "LaLiga"),
    MALLORCA(89, "Mallorca", "LaLiga"),
    REAL_BETIS(90, "Real Betis", "LaLiga"),
    REAL_SOCIEDAD(92, "Real Sociedad", "LaLiga"),
    VILLARREAL(94, "Villarreal", "LaLiga"),
    VALENCIA(95, "Valencia", "LaLiga"),
    ALAVES(263, "Deportivo Alaves", "LaLiga"),
    ELCHE(285, "Elche", "LaLiga"),
    GIRONA(298, "Girona", "LaLiga"),
    CELTA_VIGO(558, "Celta Vigo", "LaLiga"),
    SEVILLA(559, "Sevilla", "LaLiga"),
    REAL_OVIEDO(1048, "Real Oviedo", "LaLiga"),

    // Premier League
    ARSENAL(57, "Arsenal", "Premier League"),
    ASTON_VILLA(58, "Aston Villa", "Premier League"),
    CHELSEA(61, "Chelsea", "Premier League"),
    EVERTON(62, "Everton", "Premier League"),
    FULHAM(63, "Fulham", "Premier League"),
    LIVERPOOL(64, "Liverpool", "Premier League"),
    MANCHESTER_CITY(65, "Man City", "Premier League"),
    MANCHESTER_UNITED(66, "Man Utd", "Premier League"),
    NEWCASTLE(67, "Newcastle", "Premier League"),
    SUNDERLAND(71, "Sunderland", "Premier League"),
    TOTTENHAM(73, "Tottenham", "Premier League"),
    WOLVES(76, "Wolves", "Premier League"),
    BURNLEY(328, "Burnley", "Premier League"),
    LEEDS(341, "Leeds", "Premier League"),
    NOTTINGHAM_FOREST(351, "Nottingham Forest", "Premier League"),
    CRYSTAL_PALACE(354, "Crystal Palace", "Premier League"),
    BRIGHTON(397, "Brighton", "Premier League"),
    BRENTFORD(402, "Brentford", "Premier League"),
    WEST_HAM(563, "West Ham", "Premier League"),
    BOURNEMOUTH(1044, "Bournemouth", "Premier League"),

    // Serie A
    MILAN(98, "AC Milan", "Serie A"),
    FIORENTINA(99, "Fiorentina", "Serie A"),
    ROMA(100, "Roma", "Serie A"),
    ATALANTA(102, "Atalanta", "Serie A"),
    BOLOGNA(103, "Bologna", "Serie A"),
    CAGLIARI(104, "Cagliari", "Serie A"),
    GENOA(107, "Genoa", "Serie A"),
    INTER(108, "Inter", "Serie A"),
    JUVENTUS(109, "Juventus", "Serie A"),
    LAZIO(110, "Lazio", "Serie A"),
    PARMA(112, "Parma Calcio 1913", "Serie A"),
    NAPOLI(113, "Napoli", "Serie A"),
    UDINESE(115, "Udinese", "Serie A"),
    VERONA(450, "Verona", "Serie A"),
    CREMONESE(457, "Cremonese", "Serie A"),
    SASSUOLO(471, "Sassuolo", "Serie A"),
    PISA(487, "Pisa", "Serie A"),
    TORINO(586, "Torino", "Serie A"),
    LECCE(5890, "Lecce", "Serie A"),
    COMO(7397, "Como", "Serie A"),

    // Bundesliga
    KOLN(1, "FC Koln", "Bundesliga"),
    HOFFENHEIM(2, "Hoffenheim", "Bundesliga"),
    LEVERKUSEN(3, "Leverkusen", "Bundesliga"),
    DORTMUND(4, "Borussia Dortmund", "Bundesliga"),
    BAYERN(5, "Bayern", "Bundesliga"),
    HAMBURG(7, "Hamburg", "Bundesliga"),
    STUTTGART(10, "Stuttgart", "Bundesliga"),
    WOLFSBURG(11, "Wolfsburg", "Bundesliga"),
    WERDER_BREMEN(12, "Werder Bremen", "Bundesliga"),
    MAINZ(15, "Mainz", "Bundesliga"),
    AUGSBURG(16, "Augsburg", "Bundesliga"),
    FREIBURG(17, "Freiburg", "Bundesliga"),
    MONCHENGLADBACH(18, "Borussia M.Gladbach", "Bundesliga"),
    FRANKFURT(19, "Eintracht Frankfurt", "Bundesliga"),
    ST_PAULI(20, "St. Pauli", "Bundesliga"),
    UNION_BERLIN(28, "Union Berlin", "Bundesliga"),
    HEIDENHEIM(44, "FC Heidenheim", "Bundesliga"),
    RB_LEIPZIG(721, "RBL", "Bundesliga"),

    // Ligue 1
    TOULOUSE(511, "Toulouse", "Ligue 1"),
    BREST(512, "Brest", "Ligue 1"),
    MARSEILLE(516, "Marseille", "Ligue 1"),
    AUXERRE(519, "Auxerre", "Ligue 1"),
    LILLE(521, "Lille", "Ligue 1"),
    NICE(522, "Nice", "Ligue 1"),
    LYON(523, "Lyon", "Ligue 1"),
    PSG(524, "PSG", "Ligue 1"),
    LORIENT(525, "Lorient", "Ligue 1"),
    RENNES(529, "Rennes", "Ligue 1"),
    ANGERS(532, "Angers", "Ligue 1"),
    LE_HAVRE(533, "Le Havre", "Ligue 1"),
    NANTES(543, "Nantes", "Ligue 1"),
    METZ(545, "Metz", "Ligue 1"),
    LENS(546, "Lens", "Ligue 1"),
    MONACO(548, "Monaco", "Ligue 1"),
    STRASBOURG(576, "Strasbourg", "Ligue 1"),
    PARIS_FC(1045, "Paris FC", "Ligue 1");

    private final int id;
    private final String name;
    private final String league;

    TeamEnum(int id, String name, String league) {
        this.id = id;
        this.name = name;
        this.league = league;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLeague() {
        return league;
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
