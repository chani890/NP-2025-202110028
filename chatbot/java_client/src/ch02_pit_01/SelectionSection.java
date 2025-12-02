package ch02_pit_01;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

final class SelectionSection {
    interface Listener {
        void onPlanRequest(PlanRequestPayload payload);
    }

    private final ChatbotPlannerClientGUI parent;
    private final RegionRepository regionRepository;
    private final Listener listener;
    private final DateTimeFormatter dateFormatter;
    private final ZoneId systemZone;
    private final String[] themes;

    private final DefaultListModel<Region> regionModel = new DefaultListModel<>();
    private final JList<Region> regionList = new JList<>(regionModel);
    private final List<JToggleButton> themeButtons = new ArrayList<>();
    private final JSpinner startDateSpinner = createDateSpinner();
    private final JSpinner endDateSpinner = createDateSpinner();
    private final JSpinner daysSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 14, 1));
    private final JTextField birthYearField = new JTextField(6);

    SelectionSection(
        ChatbotPlannerClientGUI parent,
        RegionRepository regionRepository,
        String[] themes,
        DateTimeFormatter dateFormatter,
        ZoneId systemZone,
        Listener listener
    ) {
        this.parent = parent;
        this.regionRepository = regionRepository;
        this.listener = listener;
        this.themes = themes;
        this.dateFormatter = dateFormatter;
        this.systemZone = systemZone;
        birthYearField.setHorizontalAlignment(JTextField.CENTER);
    }

    JPanel buildPanel(Dimension cardSize) {
        JPanel wrapper = new JPanel(new GridLayout(1, 3, 12, 0));
        wrapper.add(buildRegionPanel(cardSize));
        wrapper.add(buildThemePanel(cardSize));
        wrapper.add(buildTravelerPanel(cardSize));
        return wrapper;
    }

    void loadRegions() {
        List<Region> regions = regionRepository.fetchRegions();
        regionModel.clear();
        for (Region region : regions) {
            regionModel.addElement(region);
        }
        if (!regions.isEmpty()) {
            regionList.setSelectedIndex(0);
        }
    }

    void initializeDates() {
        LocalDate today = LocalDate.now();
        setSpinnerDate(startDateSpinner, today);
        int initialDays = Math.max(1, ((Number) daysSpinner.getValue()).intValue());
        setSpinnerDate(endDateSpinner, today.plusDays(initialDays - 1));
        syncDaysWithDates();
    }

    void registerDateSynchronizers() {
        startDateSpinner.addChangeListener(e -> syncDaysWithDates());
        endDateSpinner.addChangeListener(e -> syncDaysWithDates());
        daysSpinner.addChangeListener(e -> syncDatesWithDays());
    }

    void requestPlan() {
        PlanRequestPayload payload = buildPayload();
        if (payload != null) {
            listener.onPlanRequest(payload);
        }
    }

    private JPanel buildRegionPanel(Dimension cardSize) {
        JPanel panel = new RoundedPanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = createSectionTitle("지역 선택");
        title.setFont(title.getFont().deriveFont(18f));
        panel.add(title, BorderLayout.NORTH);

        regionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        regionList.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
        regionList.setVisibleRowCount(4);
        regionList.setCellRenderer(new RegionListCellRenderer());
        JScrollPane scrollPane = new JScrollPane(regionList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(0, 110));
        panel.add(scrollPane, BorderLayout.CENTER);

        return applyCardSize(panel, cardSize);
    }

    private JPanel buildThemePanel(Dimension cardSize) {
        JPanel panel = new RoundedPanel();
        panel.setLayout(new BorderLayout(0, 16));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = createSectionTitle("테마 선택");
        title.setFont(title.getFont().deriveFont(18f));
        panel.add(title, BorderLayout.NORTH);

        themeButtons.clear();
        JPanel grid = new JPanel(new GridLayout(4, 2, 8, 8));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        for (String theme : themes) {
            JToggleButton toggle = createThemeButton(theme);
            toggle.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
            themeButtons.add(toggle);
            grid.add(toggle);
        }
        panel.add(grid, BorderLayout.CENTER);

        return applyCardSize(panel, cardSize);
    }

    private JPanel buildTravelerPanel(Dimension cardSize) {
        JPanel panel = new RoundedPanel();
        panel.setLayout(new BorderLayout(0, 16));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = createSectionTitle("정보 입력");
        title.setFont(title.getFont().deriveFont(18f));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BorderLayout());
        form.add(buildAgeInputSection(), BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);

        return applyCardSize(panel, cardSize);
    }

    private JPanel buildAgeInputSection() {
        JPanel container = new JPanel(new BorderLayout(0, 8));
        container.setOpaque(false);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);

        JLabel label = new JLabel("태어난 연도");
        label.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        row.add(label);

        birthYearField.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        birthYearField.setColumns(6);
        row.add(birthYearField);
        container.add(row, BorderLayout.NORTH);
        return container;
    }

    private JToggleButton createThemeButton(String theme) {
        JToggleButton toggle = new JToggleButton(theme);
        toggle.setFont(new Font("Malgun Gothic", Font.PLAIN, 18));
        toggle.setBackground(new Color(241, 245, 255));
        toggle.setFocusPainted(false);
        toggle.addActionListener(e -> enforceSingleThemeSelection(toggle));
        toggle.addChangeListener(e -> {
            if (toggle.isSelected()) {
                toggle.setBackground(new Color(33, 111, 237));
                toggle.setForeground(Color.WHITE);
            } else {
                toggle.setBackground(new Color(241, 245, 255));
                toggle.setForeground(Color.BLACK);
            }
        });
        return toggle;
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        return label;
    }

    private JPanel applyCardSize(JPanel panel, Dimension size) {
        panel.setPreferredSize(size);
        panel.setMinimumSize(size);
        panel.setMaximumSize(size);
        return panel;
    }

    private void enforceSingleThemeSelection(JToggleButton source) {
        if (!source.isSelected()) {
            return;
        }
        for (JToggleButton button : themeButtons) {
            if (button != source) {
                button.setSelected(false);
            }
        }
    }

    private PlanRequestPayload buildPayload() {
        Region region = regionList.getSelectedValue();
        if (region == null) {
            JOptionPane.showMessageDialog(parent, "여행 지역을 선택해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        LocalDate startDate = spinnerToLocalDate(startDateSpinner);
        LocalDate endDate = spinnerToLocalDate(endDateSpinner);
        if (endDate.isBefore(startDate)) {
            JOptionPane.showMessageDialog(parent, "종료일은 시작일 이후로 선택해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days <= 0) {
            days = 1;
        }
        daysSpinner.setValue(days);

        int birthYear;
        try {
            birthYear = parseBirthYear();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "입력 오류", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        int currentYear = LocalDate.now().getYear();
        int representativeAge = Math.max(1, currentYear - birthYear + 1);
        List<Integer> ages = Collections.singletonList(representativeAge);
        List<String> selectedThemes = themeButtons.stream()
            .filter(JToggleButton::isSelected)
            .map(AbstractButton::getText)
            .collect(Collectors.toList());
        if (selectedThemes.isEmpty()) {
            selectedThemes = Arrays.asList(themes);
        }

        return new PlanRequestPayload(
            region,
            days,
            representativeAge,
            selectedThemes,
            ages,
            formatDate(startDate),
            formatDate(endDate)
        );
    }

    private int parseBirthYear() {
        String text = birthYearField.getText().trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("태어난 연도를 입력해주세요.");
        }
        try {
            int year = Integer.parseInt(text);
            int currentYear = LocalDate.now().getYear();
            if (year < 1900 || year > currentYear) {
                throw new IllegalArgumentException("태어난 연도는 1900~" + currentYear + " 사이여야 합니다.");
            }
            return year;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("태어난 연도를 숫자로 입력해주세요.");
        }
    }

    private void syncDaysWithDates() {
        LocalDate start = spinnerToLocalDate(startDateSpinner);
        LocalDate end = spinnerToLocalDate(endDateSpinner);
        if (end.isBefore(start)) {
            end = start;
            setSpinnerDate(endDateSpinner, end);
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        int current = ((Number) daysSpinner.getValue()).intValue();
        if (days != current) {
            daysSpinner.setValue((int) days);
        }
    }

    private void syncDatesWithDays() {
        int days = Math.max(1, ((Number) daysSpinner.getValue()).intValue());
        LocalDate start = spinnerToLocalDate(startDateSpinner);
        setSpinnerDate(endDateSpinner, start.plusDays(days - 1));
    }

    private JSpinner createDateSpinner() {
        LocalDate today = LocalDate.now();
        Date initial = Date.from(today.atStartOfDay(systemZone).toInstant());
        SpinnerDateModel model = new SpinnerDateModel(initial, null, null, Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        spinner.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
        return spinner;
    }

    private LocalDate spinnerToLocalDate(JSpinner spinner) {
        Date date = (Date) spinner.getValue();
        return Instant.ofEpochMilli(date.getTime()).atZone(systemZone).toLocalDate();
    }

    private void setSpinnerDate(JSpinner spinner, LocalDate date) {
        Date converted = Date.from(date.atStartOfDay(systemZone).toInstant());
        spinner.setValue(converted);
    }

    private String formatDate(LocalDate date) {
        return dateFormatter.format(date);
    }
}

