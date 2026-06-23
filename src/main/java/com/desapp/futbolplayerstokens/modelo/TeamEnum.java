package com.desapp.futbolplayerstokens.modelo;

import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;

public enum TeamEnum {

    // LaLiga
    ATHLETIC_CLUB(77, "Athletic Club", LeagueConstant.LALIGA),
    ATLETICO_MADRID(78, "Atletico", LeagueConstant.LALIGA),
    OSASUNA(79, "Osasuna", LeagueConstant.LALIGA),
    ESPANYOL(80, "Espanyol", LeagueConstant.LALIGA),
    BARCELONA(81, "Barcelona", LeagueConstant.LALIGA),
    GETAFE(82, "Getafe", LeagueConstant.LALIGA),
    REAL_MADRID(86, "Real Madrid", LeagueConstant.LALIGA),
    RAYO_VALLECANO(87, "Rayo Vallecano", LeagueConstant.LALIGA),
    LEVANTE(88, "Levante", LeagueConstant.LALIGA),
    MALLORCA(89, "Mallorca", LeagueConstant.LALIGA),
    REAL_BETIS(90, "Real Betis", LeagueConstant.LALIGA),
    REAL_SOCIEDAD(92, "Real Sociedad", LeagueConstant.LALIGA),
    VILLARREAL(94, "Villarreal", LeagueConstant.LALIGA),
    VALENCIA(95, "Valencia", LeagueConstant.LALIGA),
    ALAVES(263, "Deportivo Alaves", LeagueConstant.LALIGA),
    ELCHE(285, "Elche", LeagueConstant.LALIGA),
    GIRONA(298, "Girona", LeagueConstant.LALIGA),
    CELTA_VIGO(558, "Celta Vigo", LeagueConstant.LALIGA),
    SEVILLA(559, "Sevilla", LeagueConstant.LALIGA),
    REAL_OVIEDO(1048, "Real Oviedo", LeagueConstant.LALIGA),

    // Premier League
    ARSENAL(57, "Arsenal", LeagueConstant.PREMIER_LEAGUE),
    ASTON_VILLA(58, "Aston Villa", LeagueConstant.PREMIER_LEAGUE),
    CHELSEA(61, "Chelsea", LeagueConstant.PREMIER_LEAGUE),
    EVERTON(62, "Everton", LeagueConstant.PREMIER_LEAGUE),
    FULHAM(63, "Fulham", LeagueConstant.PREMIER_LEAGUE),
    LIVERPOOL(64, "Liverpool", LeagueConstant.PREMIER_LEAGUE),
    MANCHESTER_CITY(65, "Man City", LeagueConstant.PREMIER_LEAGUE),
    MANCHESTER_UNITED(66, "Man Utd", LeagueConstant.PREMIER_LEAGUE),
    NEWCASTLE(67, "Newcastle", LeagueConstant.PREMIER_LEAGUE),
    SUNDERLAND(71, "Sunderland", LeagueConstant.PREMIER_LEAGUE),
    TOTTENHAM(73, "Tottenham", LeagueConstant.PREMIER_LEAGUE),
    WOLVES(76, "Wolves", LeagueConstant.PREMIER_LEAGUE),
    BURNLEY(328, "Burnley", LeagueConstant.PREMIER_LEAGUE),
    LEEDS(341, "Leeds", LeagueConstant.PREMIER_LEAGUE),
    NOTTINGHAM_FOREST(351, "Nottingham Forest", LeagueConstant.PREMIER_LEAGUE),
    CRYSTAL_PALACE(354, "Crystal Palace", LeagueConstant.PREMIER_LEAGUE),
    BRIGHTON(397, "Brighton", LeagueConstant.PREMIER_LEAGUE),
    BRENTFORD(402, "Brentford", LeagueConstant.PREMIER_LEAGUE),
    WEST_HAM(563, "West Ham", LeagueConstant.PREMIER_LEAGUE),
    BOURNEMOUTH(1044, "Bournemouth", LeagueConstant.PREMIER_LEAGUE),

    // Serie A
    MILAN(98, "AC Milan", LeagueConstant.SERIE_A),
    FIORENTINA(99, "Fiorentina", LeagueConstant.SERIE_A),
    ROMA(100, "Roma", LeagueConstant.SERIE_A),
    ATALANTA(102, "Atalanta", LeagueConstant.SERIE_A),
    BOLOGNA(103, "Bologna", LeagueConstant.SERIE_A),
    CAGLIARI(104, "Cagliari", LeagueConstant.SERIE_A),
    GENOA(107, "Genoa", LeagueConstant.SERIE_A),
    INTER(108, "Inter", LeagueConstant.SERIE_A),
    JUVENTUS(109, "Juventus", LeagueConstant.SERIE_A),
    LAZIO(110, "Lazio", LeagueConstant.SERIE_A),
    PARMA(112, "Parma Calcio 1913", LeagueConstant.SERIE_A),
    NAPOLI(113, "Napoli", LeagueConstant.SERIE_A),
    UDINESE(115, "Udinese", LeagueConstant.SERIE_A),
    VERONA(450, "Verona", LeagueConstant.SERIE_A),
    CREMONESE(457, "Cremonese", LeagueConstant.SERIE_A),
    SASSUOLO(471, "Sassuolo", LeagueConstant.SERIE_A),
    PISA(487, "Pisa", LeagueConstant.SERIE_A),
    TORINO(586, "Torino", LeagueConstant.SERIE_A),
    LECCE(5890, "Lecce", LeagueConstant.SERIE_A),
    COMO(7397, "Como", LeagueConstant.SERIE_A),

    // Bundesliga
    KOLN(1, "FC Koln", LeagueConstant.BUNDESLIGA),
    HOFFENHEIM(2, "Hoffenheim", LeagueConstant.BUNDESLIGA),
    LEVERKUSEN(3, "Leverkusen", LeagueConstant.BUNDESLIGA),
    DORTMUND(4, "Borussia Dortmund", LeagueConstant.BUNDESLIGA),
    BAYERN(5, "Bayern", LeagueConstant.BUNDESLIGA),
    HAMBURG(7, "Hamburg", LeagueConstant.BUNDESLIGA),
    STUTTGART(10, "Stuttgart", LeagueConstant.BUNDESLIGA),
    WOLFSBURG(11, "Wolfsburg", LeagueConstant.BUNDESLIGA),
    WERDER_BREMEN(12, "Werder Bremen", LeagueConstant.BUNDESLIGA),
    MAINZ(15, "Mainz", LeagueConstant.BUNDESLIGA),
    AUGSBURG(16, "Augsburg", LeagueConstant.BUNDESLIGA),
    FREIBURG(17, "Freiburg", LeagueConstant.BUNDESLIGA),
    MONCHENGLADBACH(18, "Borussia M.Gladbach", LeagueConstant.BUNDESLIGA),
    FRANKFURT(19, "Eintracht Frankfurt", LeagueConstant.BUNDESLIGA),
    ST_PAULI(20, "St. Pauli", LeagueConstant.BUNDESLIGA),
    UNION_BERLIN(28, "Union Berlin", LeagueConstant.BUNDESLIGA),
    HEIDENHEIM(44, "FC Heidenheim", LeagueConstant.BUNDESLIGA),
    RB_LEIPZIG(721, "RBL", LeagueConstant.BUNDESLIGA),

    // Ligue 1
    TOULOUSE(511, "Toulouse", LeagueConstant.LIGUE_1),
    BREST(512, "Brest", LeagueConstant.LIGUE_1),
    MARSEILLE(516, "Marseille", LeagueConstant.LIGUE_1),
    AUXERRE(519, "Auxerre", LeagueConstant.LIGUE_1),
    LILLE(521, "Lille", LeagueConstant.LIGUE_1),
    NICE(522, "Nice", LeagueConstant.LIGUE_1),
    LYON(523, "Lyon", LeagueConstant.LIGUE_1),
    PSG(524, "PSG", LeagueConstant.LIGUE_1),
    LORIENT(525, "Lorient", LeagueConstant.LIGUE_1),
    RENNES(529, "Rennes", LeagueConstant.LIGUE_1),
    ANGERS(532, "Angers", LeagueConstant.LIGUE_1),
    LE_HAVRE(533, "Le Havre", LeagueConstant.LIGUE_1),
    NANTES(543, "Nantes", LeagueConstant.LIGUE_1),
    METZ(545, "Metz", LeagueConstant.LIGUE_1),
    LENS(546, "Lens", LeagueConstant.LIGUE_1),
    MONACO(548, "Monaco", LeagueConstant.LIGUE_1),
    STRASBOURG(576, "Strasbourg", LeagueConstant.LIGUE_1),
    PARIS_FC(1045, "Paris FC", LeagueConstant.LIGUE_1),

    //WC
    URUGUAY(758, "Uruguay", LeagueConstant.WORLD_CUP),
    GERMANY(759, "Germany", LeagueConstant.WORLD_CUP),
    SPAIN(760, "Spain", LeagueConstant.WORLD_CUP),
    PARAGUAY(761, "Paraguay", LeagueConstant.WORLD_CUP),
    ARGENTINA(762, "Argentina", LeagueConstant.WORLD_CUP),
    GHANA(763, "Ghana", LeagueConstant.WORLD_CUP),
    BRAZIL(764, "Brazil", LeagueConstant.WORLD_CUP),
    PORTUGAL(765, "Portugal", LeagueConstant.WORLD_CUP),
    JAPAN(766, "Japan", LeagueConstant.WORLD_CUP),
    MEXICO(769, "Mexico", LeagueConstant.WORLD_CUP),
    ENGLAND(770, "England", LeagueConstant.WORLD_CUP),
    UNITED_STATES(771, "United States", LeagueConstant.WORLD_CUP),
    SOUTH_KOREA(772, "South Korea", LeagueConstant.WORLD_CUP),
    FRANCE(773, "France", LeagueConstant.WORLD_CUP),
    SOUTH_AFRICA(774, "South Africa", LeagueConstant.WORLD_CUP),
    ALGERIA(778, "Algeria", LeagueConstant.WORLD_CUP),
    AUSTRALIA(779, "Australia", LeagueConstant.WORLD_CUP),
    NEW_ZEALAND(783, "New Zealand", LeagueConstant.WORLD_CUP),
    SWITZERLAND(788, "Switzerland", LeagueConstant.WORLD_CUP),
    ECUADOR(791, "Ecuador", LeagueConstant.WORLD_CUP),
    SWEDEN(792, "Sweden", LeagueConstant.WORLD_CUP),
    CZECHIA(798, "Czechia", LeagueConstant.WORLD_CUP),
    CROATIA(799, "Croatia", LeagueConstant.WORLD_CUP),
    SAUDI_ARABIA(801, "Saudi Arabia", LeagueConstant.WORLD_CUP),
    TUNISIA(802, "Tunisia", LeagueConstant.WORLD_CUP),
    TURKEY(803, "Turkey", LeagueConstant.WORLD_CUP),
    SENEGAL(804, "Senegal", LeagueConstant.WORLD_CUP),
    BELGIUM(805, "Belgium", LeagueConstant.WORLD_CUP),
    MOROCCO(815, "Morocco", LeagueConstant.WORLD_CUP),
    AUSTRIA(816, "Austria", LeagueConstant.WORLD_CUP),
    COLOMBIA(818, "Colombia", LeagueConstant.WORLD_CUP),
    EGYPT(825, "Egypt", LeagueConstant.WORLD_CUP),
    CANADA(828, "Canada", LeagueConstant.WORLD_CUP),
    HAITI(836, "Haiti", LeagueConstant.WORLD_CUP),
    IRAN(840, "Iran", LeagueConstant.WORLD_CUP),
    BOSNIA_HERZEGOVINA(1060, "Bosnia-Herzegovina", LeagueConstant.WORLD_CUP),
    PANAMA(1836, "Panama", LeagueConstant.WORLD_CUP),
    CAPE_VERDE_ISLANDS(1930, "Cape Verde Islands", LeagueConstant.WORLD_CUP),
    CONGO_DR(1934, "DR Congo", LeagueConstant.WORLD_CUP),
    IVORY_COAST(1935, "Ivory Coast", LeagueConstant.WORLD_CUP),
    QATAR(8030, "Qatar", LeagueConstant.WORLD_CUP),
    JORDAN(8049, "Jordan", LeagueConstant.WORLD_CUP),
    IRAQ(8062, "Iraq", LeagueConstant.WORLD_CUP),
    UZBEKISTAN(8070, "Uzbekistan", LeagueConstant.WORLD_CUP),
    NETHERLANDS(8601, "Netherlands", LeagueConstant.WORLD_CUP),
    NORWAY(8872, "Norway", LeagueConstant.WORLD_CUP),
    SCOTLAND(8873, "Scotland", LeagueConstant.WORLD_CUP),
    CURACAO(9460, "Curaçao", LeagueConstant.WORLD_CUP);

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
        throw new ResourceNotFoundException("No team with id " + id);
    }
}
