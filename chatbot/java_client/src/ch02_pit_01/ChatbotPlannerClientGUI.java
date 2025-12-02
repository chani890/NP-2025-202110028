package ch02_pit_01;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.script.ScriptException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 여행 챗봇 TCP 클라이언트 GUI.
 */
public class ChatbotPlannerClientGUI extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(ChatbotPlannerClientGUI.class.getName());
    private static final String DEFAULT_HOST = System.getProperty("planner.server.host", "127.0.0.1");
    private static final int DEFAULT_PORT = parsePort(System.getProperty("planner.server.port", "7777"));
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(parseTimeoutSeconds());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    private static final Dimension SELECTION_CARD_SIZE = new Dimension(340, 260);
    private static final Color USER_CHAT_COLOR = new Color(209, 233, 255);
    private static final Color SERVER_CHAT_COLOR = new Color(244, 246, 250);
    private static final Color SYSTEM_CHAT_COLOR = new Color(235, 238, 245);
    private static final String[] THEMES = {
         "자연관광","역사관광","체험관광","레저스포츠","쇼핑","음식","숙박","기타관광"
    };

    private final RegionRepository regionRepository = new RegionRepository();
    private final String serverHost = DEFAULT_HOST;
    private final int serverPort = DEFAULT_PORT;

    private final JButton requestButton = new JButton("플랜 생성하기");
    private final JTextArea resultArea = new JTextArea(12, 36);
    private final CardLayout resultCardLayout = new CardLayout();
    private final JPanel resultCardPanel = new JPanel(resultCardLayout);
    private final JLabel statusLabel = new JLabel("준비 완료");

    private final PlannerViewContext viewContext;
    private final SelectionSection selectionSection;
    private final PlanResultSection planResultSection;
    private final PlannerNetworkController networkController;

    private JFrame resultFrame;
    private String lastSelectedPlanId;
    private PlanRequestPayload lastPayload;
    private String lastSessionId;

    public ChatbotPlannerClientGUI() {
        super("Chatbot Planner TCP Client");
        this.viewContext = new PlannerViewContext(this, statusLabel, resultCardLayout, resultCardPanel, resultArea);
        this.selectionSection = new SelectionSection(
            this,
            regionRepository,
            THEMES,
            DATE_FORMATTER,
            SYSTEM_ZONE,
            this::submitPlanRequest
        );
        this.planResultSection = new PlanResultSection(
            viewContext,
            USER_CHAT_COLOR,
            SERVER_CHAT_COLOR,
            SYSTEM_CHAT_COLOR,
            new PlanResultSection.Listener() {
                @Override
                public void onPlanSelectionRequested(String planId, String planTitle) {
                    handlePlanSelectionRequest(planId, planTitle);
                }

                @Override
                public void onDetailRequestSubmitted(String userRequest) {
                    handleDetailRequestSubmitted(userRequest);
                }
            }
        );
        this.networkController = new PlannerNetworkController(DEFAULT_TIMEOUT, serverHost, serverPort);
        initUi();
        selectionSection.loadRegions();
        selectionSection.initializeDates();
        selectionSection.registerDateSynchronizers();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeTcpClient();
            }
        });
        connectToServerOnStartup();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));
        setBorderPadding();

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 12));
        centerWrapper.setOpaque(false);
        centerWrapper.add(selectionSection.buildPanel(SELECTION_CARD_SIZE), BorderLayout.CENTER);
        centerWrapper.add(buildActionPanel(), BorderLayout.SOUTH);

        add(centerWrapper, BorderLayout.CENTER);
        add(buildStatusPanel(), BorderLayout.NORTH);
        configureResultFrame();

        setMinimumSize(new Dimension(1180, 720));
        setPreferredSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);
    }

    private void setBorderPadding() {
        java.awt.Container container = getContentPane();
        if (container instanceof JComponent component) {
            component.setBorder(new EmptyBorder(16, 16, 16, 16));
        }
    }

    private void configureResultFrame() {
        if (resultFrame != null) {
            return;
        }
        resultFrame = new JFrame("서버 응답");
        resultFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        resultFrame.setContentPane(buildResultPanel());
        resultFrame.pack();
        resultFrame.setSize(900, 520);
        resultFrame.setLocationRelativeTo(this);
        viewContext.setResultFrame(resultFrame);
    }

    private void showJsonResponse(String raw) {
        viewContext.ensureResultFrameVisible();
        viewContext.getResultArea().setText(JsonPrettyPrinter.format(raw));
        viewContext.showJsonCard();
    }

    private boolean renderPlanRecommendations(String raw) {
        try {
            Map<String, Object> json = parseJsonObject(raw);
            String type = String.valueOf(json.getOrDefault("type", ""));
            if ("plan_recommendations".equals(type)) {
                updateSessionId(json);
                List<PlanEntry> entries = planResultSection.buildPlanEntries(json);
                planResultSection.renderPlanEntries(entries);
                String source = PlanResultSection.valueOrDefault(json.get("source"), "");
                if ("fallback".equalsIgnoreCase(source)) {
                    planResultSection.setPlanHelperText("AI 생성에 실패하여 기본 일정을 표시합니다.");
                } else if (entries.isEmpty()) {
                    planResultSection.setPlanHelperText("추천된 플랜이 없습니다.");
                } else {
                    planResultSection.setPlanHelperText("추천 일정 " + entries.size() + "개를 확인해보세요.");
                }
                return true;
            } else if ("plan_final".equals(type)) {
                Object planObj = json.get("final_plan");
                if (!(planObj instanceof Map<?, ?>)) {
                    planObj = json.get("plan");
                }
                if (planObj instanceof Map<?, ?> map) {
                    planResultSection.updateFinalPlan(map, json);
                    return true;
                }
            } else if ("error".equals(type)) {
                JOptionPane.showMessageDialog(
                    this,
                    PlanResultSection.valueOrDefault(json.get("message"), "알 수 없는 오류가 발생했습니다."),
                    "서버 오류",
                    JOptionPane.ERROR_MESSAGE
                );
                showJsonResponse(raw);
                return true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "플랜 추천 화면으로 전환하지 못했습니다.", ex);
        }
        return false;
    }

    private void updateSessionId(Map<String, Object> json) {
        Object value = json.get("session_id");
        if (value == null) {
            return;
        }
        String session = String.valueOf(value).trim();
        if (!session.isEmpty()) {
            lastSessionId = session;
        }
    }

    private Map<String, Object> parseJsonObject(String raw) throws ScriptException {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Object parsed = SimpleJsonParser.parse(raw);
            if (parsed instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>) map;
                return casted;
            }
        } catch (IllegalArgumentException ex) {
            throw new ScriptException("JSON Parse Error: " + ex.getMessage());
        }
        return Collections.emptyMap();
    }

    private JPanel buildActionPanel() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(new EmptyBorder(12, 0, 12, 0));
        wrapper.setOpaque(false);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonRow.setOpaque(false);
        requestButton.addActionListener(e -> selectionSection.requestPlan());
        requestButton.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        requestButton.setBackground(new Color(33, 111, 237));
        requestButton.setForeground(Color.WHITE);
        requestButton.setFocusPainted(false);
        requestButton.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        requestButton.setPreferredSize(new Dimension(260, 46));
        buttonRow.add(requestButton);
        wrapper.add(buttonRow);

        return wrapper;
    }

    private void connectToServerOnStartup() {
        requestButton.setEnabled(false);
        statusLabel.setText("서버에 연결 중입니다...");
        networkController.connectAsync(
            () -> statusLabel.setText("서버 연결 완료"),
            ex -> {
                statusLabel.setText("서버 연결 실패");
                LOGGER.log(Level.WARNING, "서버 연결 실패", ex);
                JOptionPane.showMessageDialog(
                    ChatbotPlannerClientGUI.this,
                    "서버에 연결할 수 없습니다. 서버가 실행 중인지 확인한 뒤 다시 시도해주세요.",
                    "연결 오류",
                    JOptionPane.ERROR_MESSAGE
                );
            },
            () -> requestButton.setEnabled(true)
        );
    }

    private JPanel buildResultPanel() {
        JPanel jsonPanel = new JPanel(new BorderLayout());
        jsonPanel.setBorder(new TitledBorder(BorderFactory.createEmptyBorder(), "서버 응답 (JSON)"));
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        resultArea.setLineWrap(false);
        jsonPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JPanel planPanel = planResultSection.buildPanel();

        resultCardPanel.add(jsonPanel, "json");
        resultCardPanel.add(planPanel, "plans");
        resultCardLayout.show(resultCardPanel, "json");
        return resultCardPanel;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
    }

    private void submitPlanRequest(PlanRequestPayload payload) {
        lastPayload = payload;
        lastSessionId = null;
        requestButton.setEnabled(false);
        statusLabel.setText("서버에서 플랜을 생성하는 중입니다...");
        viewContext.getResultArea().setText("플랜 생성 중...\n");
        viewContext.ensureResultFrameVisible();
        viewContext.showJsonCard();

        networkController.requestPlan(
            payload,
            raw -> {
                if (!renderPlanRecommendations(raw)) {
                    showJsonResponse(raw);
                }
                statusLabel.setText("완료");
            },
            ex -> {
                statusLabel.setText("오류 발생");
                showJsonResponse("요청 중 오류: " + ex.getMessage());
                LOGGER.log(Level.WARNING, "플랜 요청 실패", ex);
            },
            () -> requestButton.setEnabled(true)
        );
    }

    private boolean hasActiveSession() {
        return lastSessionId != null && !lastSessionId.isBlank();
    }

    private boolean isPlanSelectionReady() {
        return lastPayload != null && hasActiveSession();
    }

    private void handlePlanSelectionRequest(String planId, String planTitle) {
        if (planId == null || planId.isBlank()) {
            JOptionPane.showMessageDialog(
                this,
                "선택할 플랜 정보를 찾을 수 없습니다.",
                "선택 오류",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        if (!isPlanSelectionReady()) {
            JOptionPane.showMessageDialog(
                this,
                "먼저 플랜을 생성한 뒤 다시 시도해주세요.",
                "선택 불가",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        planResultSection.appendUserMessage(planTitle == null || planTitle.isBlank()
            ? "선택한 플랜으로 세부 일정을 만들어줘."
            : planTitle + " 세부 일정을 만들어줘.");
        planResultSection.clearDetailInput();
        requestDetailedPlan(planId, "");
    }

    private void handleDetailRequestSubmitted(String userRequest) {
        if (lastSelectedPlanId == null || lastSelectedPlanId.isBlank()) {
            JOptionPane.showMessageDialog(
                this,
                "먼저 상세 플랜을 생성할 추천안을 선택해주세요.",
                "요청 불가",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        planResultSection.appendUserMessage(userRequest);
        planResultSection.clearDetailInput();
        requestDetailedPlan(lastSelectedPlanId, userRequest);
    }

    private void requestDetailedPlan(String planId, String userRequest) {
        if (planId == null || planId.isBlank()) {
            JOptionPane.showMessageDialog(
                this,
                "선택할 플랜 정보를 찾을 수 없습니다.",
                "선택 오류",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        if (!isPlanSelectionReady()) {
            JOptionPane.showMessageDialog(
                this,
                "먼저 플랜을 생성한 뒤 다시 시도해주세요.",
                "선택 불가",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        PlanSelectionPayload selectionPayload = new PlanSelectionPayload(
            lastPayload,
            lastSessionId,
            planId,
            userRequest
        );
        lastSelectedPlanId = planId;
        planResultSection.setDetailRequestEnabled(false);
        statusLabel.setText("선택한 플랜으로 최종 일정을 생성 중입니다...");
        networkController.requestPlanSelection(
            selectionPayload,
            raw -> {
                if (!renderPlanRecommendations(raw)) {
                    showJsonResponse(raw);
                }
                statusLabel.setText("완료");
            },
            ex -> {
                statusLabel.setText("오류 발생");
                showJsonResponse("요청 중 오류: " + ex.getMessage());
                LOGGER.log(Level.WARNING, "플랜 선택 실패", ex);
            },
            () -> planResultSection.setDetailRequestEnabled(true)
        );
    }

    private void closeTcpClient() {
        networkController.close();
    }

    @Override
    public void dispose() {
        closeTcpClient();
        super.dispose();
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1000 || port > 65535) {
                return 7777;
            }
            return port;
        } catch (NumberFormatException ex) {
            return 7777;
        }
    }

    private static long parseTimeoutSeconds() {
        String value = System.getProperty("planner.server.timeout.seconds", "60");
        try {
            long seconds = Long.parseLong(value);
            return seconds > 0 ? seconds : 60L;
        } catch (NumberFormatException ex) {
            return 60L;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatbotPlannerClientGUI frame = new ChatbotPlannerClientGUI();
            frame.setVisible(true);
        });
    }
}

final class SimpleJsonParser {
    private final String text;
    private int index;

    private SimpleJsonParser(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("내용이 비어 있습니다.");
        }
        SimpleJsonParser parser = new SimpleJsonParser(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw new IllegalArgumentException("예상치 못한 문자: " + parser.peek());
        }
        return value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (isEnd()) {
            throw new IllegalArgumentException("예상치 못한 JSON 종료");
        }
        char ch = peek();
        if (ch == '{') {
            return parseObject();
        }
        if (ch == '[') {
            return parseArray();
        }
        if (ch == '"') {
            return parseString();
        }
        if (ch == 't' || ch == 'f') {
            return parseBoolean();
        }
        if (ch == 'n') {
            return parseNull();
        }
        if (ch == '-' || Character.isDigit(ch)) {
            return parseNumber();
        }
        throw new IllegalArgumentException("지원하지 않는 값: " + ch);
    }

    private Map<String, Object> parseObject() {
        expect('{');
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();
        if (tryConsume('}')) {
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            result.put(key, value);
            skipWhitespace();
            if (tryConsume('}')) {
                break;
            }
            expect(',');
        }
        return result;
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (tryConsume(']')) {
            return list;
        }
        while (true) {
            list.add(parseValue());
            skipWhitespace();
            if (tryConsume(']')) {
                break;
            }
            expect(',');
        }
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (!isEnd()) {
            char ch = text.charAt(index++);
            if (ch == '"') {
                return sb.toString();
            }
            if (ch == '\\') {
                if (isEnd()) {
                    throw new IllegalArgumentException("잘못된 이스케이프 시퀀스");
                }
                char esc = text.charAt(index++);
                switch (esc) {
                    case '"', '\\', '/' -> sb.append(esc);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (index + 4 > text.length()) {
                            throw new IllegalArgumentException("잘못된 유니코드 이스케이프");
                        }
                        String hex = text.substring(index, index + 4);
                        index += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                    }
                    default -> throw new IllegalArgumentException("지원하지 않는 이스케이프: \\" + esc);
                }
            } else {
                sb.append(ch);
            }
        }
        throw new IllegalArgumentException("문자열이 닫히지 않았습니다.");
    }

    private Object parseNumber() {
        int start = index;
        if (peek() == '-') {
            index++;
        }
        while (!isEnd() && Character.isDigit(peek())) {
            index++;
        }
        if (!isEnd() && peek() == '.') {
            index++;
            while (!isEnd() && Character.isDigit(peek())) {
                index++;
            }
        }
        if (!isEnd() && (peek() == 'e' || peek() == 'E')) {
            index++;
            if (!isEnd() && (peek() == '+' || peek() == '-')) {
                index++;
            }
            while (!isEnd() && Character.isDigit(peek())) {
                index++;
            }
        }
        String numberText = text.substring(start, index);
        if (numberText.isBlank()) {
            throw new IllegalArgumentException("잘못된 숫자 형식");
        }
        if (numberText.contains(".") || numberText.contains("e") || numberText.contains("E")) {
            return Double.parseDouble(numberText);
        }
        try {
            return Long.parseLong(numberText);
        } catch (NumberFormatException ex) {
            return Double.parseDouble(numberText);
        }
    }

    private Boolean parseBoolean() {
        if (matchAhead("true")) {
            index += 4;
            return Boolean.TRUE;
        }
        if (matchAhead("false")) {
            index += 5;
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("잘못된 불리언 값");
    }

    private Object parseNull() {
        if (matchAhead("null")) {
            index += 4;
            return null;
        }
        throw new IllegalArgumentException("잘못된 null 값");
    }

    private void skipWhitespace() {
        while (!isEnd() && Character.isWhitespace(peek())) {
            index++;
        }
    }

    private void expect(char expected) {
        if (!tryConsume(expected)) {
            throw new IllegalArgumentException("문자 '" + expected + "' 가 필요합니다.");
        }
    }

    private boolean tryConsume(char expected) {
        if (!isEnd() && peek() == expected) {
            index++;
            return true;
        }
        return false;
    }

    private boolean matchAhead(String keyword) {
        return text.regionMatches(index, keyword, 0, keyword.length());
    }

    private char peek() {
        return text.charAt(index);
    }

    private boolean isEnd() {
        return index >= text.length();
    }
}

final class PlanEntry {
    private final Map<?, ?> data;
    private final String dayLabel;
    private final int dayNumber;

    PlanEntry(Map<?, ?> data, String dayLabel, int dayNumber) {
        this.data = Objects.requireNonNull(data, "data");
        this.dayLabel = dayLabel == null ? "" : dayLabel;
        this.dayNumber = Math.max(0, dayNumber);
    }

    Map<?, ?> data() {
        return data;
    }

    String dayLabel() {
        return dayLabel;
    }

    int dayNumber() {
        return dayNumber;
    }
}

final class Region {
    private final String code;
    private final String name;
    private final String parentCode;
    private final int level;

    Region(String code, String name, String parentCode, int level) {
        this.code = code;
        this.name = name;
        this.parentCode = parentCode;
        this.level = level;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public String parentCode() {
        return parentCode;
    }

    public int level() {
        return level;
    }

    @Override
    public String toString() {
        return name;
    }
}

final class RegionRepository {
    private static final Logger LOGGER = Logger.getLogger(RegionRepository.class.getName());

    List<Region> fetchRegions() {
        Optional<Connection> connection = openConnection();
        if (connection.isEmpty()) {
            return fallbackRegions();
        }

        String query = "SELECT region_code, region_name, parent_code, level FROM Region ORDER BY level, region_name";
        try (Connection conn = connection.get();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            List<Region> result = new ArrayList<>();
            while (rs.next()) {
                result.add(
                    new Region(
                        rs.getString("region_code"),
                        rs.getString("region_name"),
                        rs.getString("parent_code"),
                        rs.getInt("level")
                    )
                );
            }
            if (result.isEmpty()) {
                return fallbackRegions();
            }
            return result;
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Region 테이블 조회 실패, 기본 목록을 사용합니다.", ex);
            return fallbackRegions();
        }
    }

    private Optional<Connection> openConnection() {
        String url = firstNonBlank(
            System.getenv("TRAVEL_DB_URL"),
            System.getProperty("planner.db.url")
        );
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }

        String user = firstNonBlank(
            System.getenv("TRAVEL_DB_USER"),
            System.getProperty("planner.db.user")
        );
        String password = firstNonBlank(
            System.getenv("TRAVEL_DB_PASSWORD"),
            System.getenv("TRAVEL_DB_PASS"),
            System.getProperty("planner.db.password")
        );
        String driver = firstNonBlank(
            System.getProperty("planner.db.driver"),
            "com.mysql.cj.jdbc.Driver"
        );

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.FINE, "JDBC 드라이버를 찾을 수 없습니다: " + driver, ex);
        }

        try {
            if (user != null && password != null) {
                return Optional.of(DriverManager.getConnection(url, user, password));
            }
            if (user != null) {
                return Optional.of(DriverManager.getConnection(url, user, ""));
            }
            return Optional.of(DriverManager.getConnection(url));
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "데이터베이스 연결에 실패했습니다.", ex);
            return Optional.empty();
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private List<Region> fallbackRegions() {
        return Arrays.asList(
            new Region("1100000000", "서울특별시", null, 1),
            new Region("2600000000", "부산광역시", null, 1),
            new Region("2700000000", "대구광역시", null, 1),
            new Region("2800000000", "인천광역시", null, 1),
            new Region("2900000000", "광주광역시", null, 1),
            new Region("3000000000", "대전광역시", null, 1),
            new Region("3100000000", "울산광역시", null, 1),
            new Region("3600000000", "세종특별자치시", null, 1),
            new Region("4100000000", "경기도", null, 1),
            new Region("4200000000", "강원특별자치도", null, 1),
            new Region("4300000000", "충청북도", null, 1),
            new Region("4400000000", "충청남도", null, 1),
            new Region("4500000000", "전라북도", null, 1),
            new Region("4600000000", "전라남도", null, 1),
            new Region("4700000000", "경상북도", null, 1),
            new Region("4800000000", "경상남도", null, 1),
            new Region("5000000000", "제주특별자치도", null, 1)
        );
    }
}

final class PlanRequestPayload {
    private final Region region;
    private final int days;
    private final int age;
    private final List<String> themes;
    private final List<Integer> companionAges;
    private final String startDate;
    private final String endDate;

    PlanRequestPayload(
        Region region,
        int days,
        int age,
        List<String> themes,
        List<Integer> companionAges,
        String startDate,
        String endDate
    ) {
        this.region = Objects.requireNonNull(region, "region");
        this.days = days;
        this.age = age;
        this.themes = Collections.unmodifiableList(new ArrayList<>(themes));
        this.companionAges = Collections.unmodifiableList(new ArrayList<>(companionAges));
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        this.endDate = Objects.requireNonNull(endDate, "endDate");
    }

    Region region() {
        return region;
    }

    int days() {
        return days;
    }

    int age() {
        return age;
    }

    List<String> themes() {
        return themes;
    }

    List<Integer> companionAges() {
        return companionAges;
    }

    String startDate() {
        return startDate;
    }

    String endDate() {
        return endDate;
    }

    String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"plan_request\"");
        sb.append(",\"destination\":\"").append(escape(region.name())).append("\"");
        sb.append(",\"region_code\":\"").append(escape(region.code())).append("\"");
        sb.append(",\"days\":").append(days);
        sb.append(",\"age\":").append(age);
        sb.append(",\"theme\":[");
        for (int i = 0; i < themes.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(escape(themes.get(i))).append("\"");
        }
        sb.append("]");
        sb.append(",\"start_date\":\"").append(escape(startDate)).append("\"");
        sb.append(",\"end_date\":\"").append(escape(endDate)).append("\"");
        sb.append(",\"ages\":[");
        for (int i = 0; i < companionAges.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(companionAges.get(i));
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private String escape(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }
}

final class PlanSelectionPayload {
    private final PlanRequestPayload requestPayload;
    private final String sessionId;
    private final String selectedPlanId;
    private final String userRequest;

    PlanSelectionPayload(
        PlanRequestPayload requestPayload,
        String sessionId,
        String selectedPlanId,
        String userRequest
    ) {
        this.requestPayload = Objects.requireNonNull(requestPayload, "requestPayload");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.selectedPlanId = Objects.requireNonNull(selectedPlanId, "selectedPlanId");
        this.userRequest = userRequest == null ? "" : userRequest;
    }

    String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"plan_selection\"");
        sb.append(",\"session_id\":\"").append(escape(sessionId)).append("\"");
        sb.append(",\"selected_plan_id\":\"").append(escape(selectedPlanId)).append("\"");
        sb.append(",\"destination\":\"").append(escape(requestPayload.region().name())).append("\"");
        sb.append(",\"region_code\":\"").append(escape(requestPayload.region().code())).append("\"");
        sb.append(",\"days\":").append(requestPayload.days());
        sb.append(",\"age\":").append(requestPayload.age());
        sb.append(",\"theme\":[");
        List<String> themes = requestPayload.themes();
        for (int i = 0; i < themes.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(escape(themes.get(i))).append("\"");
        }
        sb.append("]");
        sb.append(",\"start_date\":\"").append(escape(requestPayload.startDate())).append("\"");
        sb.append(",\"end_date\":\"").append(escape(requestPayload.endDate())).append("\"");
        sb.append(",\"ages\":[");
        List<Integer> ages = requestPayload.companionAges();
        for (int i = 0; i < ages.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(ages.get(i));
        }
        sb.append("]");
        if (!userRequest.isBlank()) {
            sb.append(",\"user_request\":\"").append(escape(userRequest)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escape(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }
}

final class TcpPlanClient implements java.io.Closeable {
    private static final Logger LOGGER = Logger.getLogger(TcpPlanClient.class.getName());
    private final Duration timeout;
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private String connectedHost;
    private int connectedPort;

    TcpPlanClient(Duration timeout) {
        this.timeout = timeout == null ? Duration.ofSeconds(60) : timeout;
    }

    synchronized void connect(String host, int port) throws IOException {
        if (isConnected(host, port)) {
            return;
        }
        closeInternal();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("호스트를 입력해주세요.");
        }
        Socket newSocket = new Socket();
        BufferedWriter newWriter = null;
        BufferedReader newReader = null;
        try {
            newSocket.connect(new InetSocketAddress(host, port), (int) timeout.toMillis());
            newSocket.setSoTimeout((int) timeout.toMillis());
            newWriter = new BufferedWriter(
                new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8)
            );
            newReader = new BufferedReader(
                new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8)
            );
            this.writer = newWriter;
            this.reader = newReader;
            this.socket = newSocket;
            this.connectedHost = host;
            this.connectedPort = port;
        } catch (IOException ex) {
            closeQuietly(newWriter);
            closeQuietly(newReader);
            try {
                newSocket.close();
            } catch (IOException ignored) {
            }
            throw ex;
        }
    }

    private boolean isConnected(String host, int port) {
        return socket != null
            && socket.isConnected()
            && !socket.isClosed()
            && Objects.equals(connectedHost, host)
            && connectedPort == port;
    }

    private void ensureConnection(String host, int port) throws IOException {
        if (!isConnected(host, port)) {
            connect(host, port);
        }
    }

    synchronized String requestPlan(String host, int port, PlanRequestPayload payload) throws IOException {
        ensureConnection(host, port);
        return sendPayload(payload.toJson());
    }

    synchronized String selectPlan(String host, int port, PlanSelectionPayload payload) throws IOException {
        ensureConnection(host, port);
        return sendPayload(payload.toJson());
    }

    private String sendPayload(String jsonPayload) throws IOException {
        if (writer == null || reader == null) {
            throw new IOException("서버 연결이 초기화되지 않았습니다.");
        }
        try {
            writer.write(jsonPayload);
            writer.write("\n");
            writer.flush();
            StringBuilder response = new StringBuilder();
            boolean firstLine = true;
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("서버와의 연결이 끊어졌습니다.");
                }
                if (!firstLine) {
                    response.append(System.lineSeparator());
                }
                response.append(line);
                firstLine = false;
                if (!reader.ready()) {
                    break;
                }
            }
            if (response.length() == 0) {
                throw new IOException("서버 응답이 비어 있습니다.");
            }
            return response.toString();
        } catch (IOException ex) {
            closeInternal();
            LOGGER.log(Level.WARNING, "TCP 요청 실패", ex);
            throw ex;
        }
    }

    @Override
    public synchronized void close() {
        closeInternal();
    }

    private void closeInternal() {
        closeQuietly(reader);
        reader = null;
        closeQuietly(writer);
        writer = null;
        closeQuietly(socket);
        socket = null;
        connectedHost = null;
        connectedPort = 0;
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }
}

final class JsonPrettyPrinter {
    private JsonPrettyPrinter() {
    }

    static String format(String json) {
        if (json == null || json.isBlank()) {
            return "서버 응답이 비어 있습니다.";
        }

        StringBuilder builder = new StringBuilder();
        int indent = 0;
        boolean inQuotes = false;

        for (int i = 0; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
                builder.append(ch);
                continue;
            }

            if (inQuotes) {
                builder.append(ch);
                continue;
            }

            switch (ch) {
                case '{':
                case '[':
                    builder.append(ch);
                    indent++;
                    appendIndent(builder, indent);
                    break;
                case '}':
                case ']':
                    indent = Math.max(0, indent - 1);
                    appendIndent(builder, indent);
                    builder.append(ch);
                    break;
                case ',':
                    builder.append(ch);
                    appendIndent(builder, indent);
                    break;
                case ':':
                    builder.append(": ");
                    break;
                default:
                    if (!Character.isWhitespace(ch)) {
                        builder.append(ch);
                    }
                    break;
            }
        }
        return builder.toString().trim();
    }

    private static void appendIndent(StringBuilder builder, int indent) {
        builder.append(System.lineSeparator());
        for (int i = 0; i < indent; i++) {
            builder.append("  ");
        }
    }
}

final class RoundedPanel extends JPanel {
    RoundedPanel() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setColor(Color.WHITE);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);
        g2.setColor(new Color(220, 226, 235));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 32, 32);
        g2.dispose();
    }
}

final class RegionListCellRenderer extends DefaultListCellRenderer {
    private final Color selectedBg = new Color(33, 111, 237);
    private final Color selectedFg = Color.WHITE;
    private final Color normalBg = new Color(245, 247, 250);

    @Override
    public Component getListCellRendererComponent(
        JList<?> list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, false, false);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        label.setHorizontalAlignment(LEFT);
        label.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        label.setBackground(isSelected ? selectedBg : normalBg);
        label.setForeground(isSelected ? selectedFg : Color.BLACK);
        label.setText(value == null ? "" : value.toString());
        return label;
    }
}

