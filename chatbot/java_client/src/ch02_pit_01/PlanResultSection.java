package ch02_pit_01;

import javax.swing.*;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class PlanResultSection {
    interface Listener {
        void onPlanSelectionRequested(String planId, String planTitle);

        void onDetailRequestSubmitted(String userRequest);
    }

    private final PlannerViewContext viewContext;
    private final Listener listener;
    private final Color userChatColor;
    private final Color serverChatColor;
    private final Color systemChatColor;

    private JLabel planHelperLabel;
    private JPanel planListPanel;
    private JScrollPane planScrollPane;
    private JTextField detailRequestField;
    private JButton detailRequestButton;
    private JPanel detailControlPanel;

    PlanResultSection(
        PlannerViewContext viewContext,
        Color userChatColor,
        Color serverChatColor,
        Color systemChatColor,
        Listener listener
    ) {
        this.viewContext = viewContext;
        this.listener = listener;
        this.userChatColor = userChatColor;
        this.serverChatColor = serverChatColor;
        this.systemChatColor = systemChatColor;
    }

    JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new TitledBorder(BorderFactory.createEmptyBorder(), "플랜 결과"));
        planHelperLabel = new JLabel("AI에게 채팅으로 요청사항을 보내면 세부 플랜과 날씨 안내를 받을 수 있습니다.");
        planHelperLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        Box header = Box.createVerticalBox();
        header.add(planHelperLabel);
        header.add(Box.createVerticalStrut(6));
        panel.add(header, BorderLayout.NORTH);

        planListPanel = new JPanel();
        planListPanel.setOpaque(false);
        planListPanel.setLayout(new BoxLayout(planListPanel, BoxLayout.Y_AXIS));
        planScrollPane = new JScrollPane(planListPanel);
        planScrollPane.setBorder(BorderFactory.createEmptyBorder());
        planScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        planScrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(planScrollPane, BorderLayout.CENTER);
        appendSystemMessage("추천 결과를 받으면 이 영역에 AI 메시지가 순서대로 표시됩니다.");

        detailControlPanel = new JPanel(new BorderLayout(8, 0));
        detailControlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(224, 229, 238)),
            new EmptyBorder(8, 0, 0, 0)
        ));
        detailControlPanel.setVisible(false);

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setOpaque(false);
        JLabel requestLabel = new JLabel("요청사항");
        requestLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        detailRequestField = new JTextField(28);
        detailRequestField.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        detailRequestField.setToolTipText("예: 비 올 때 실내 코스로 바꿔줘 / 아이와 같이 가요");
        detailRequestField.addActionListener(e -> notifyDetailRequest());
        inputRow.add(requestLabel, BorderLayout.WEST);
        inputRow.add(detailRequestField, BorderLayout.CENTER);

        detailRequestButton = new JButton("전송");
        detailRequestButton.setEnabled(false);
        detailRequestButton.addActionListener(e -> notifyDetailRequest());

        detailControlPanel.add(inputRow, BorderLayout.CENTER);
        detailControlPanel.add(detailRequestButton, BorderLayout.EAST);

        panel.add(detailControlPanel, BorderLayout.SOUTH);
        return panel;
    }

    void setPlanHelperText(String text) {
        if (planHelperLabel != null) {
            planHelperLabel.setText(text);
        }
    }

    List<PlanEntry> buildPlanEntries(Map<String, Object> json) {
        if (json == null) {
            return List.of();
        }
        Object plansNode = json.get("plans");
        if (plansNode == null) {
            plansNode = json.get("options");
        }
        return extractPlanEntries(plansNode, "");
    }

    void renderPlanEntries(List<PlanEntry> entries) {
        planListPanel.removeAll();
        if (entries.isEmpty()) {
            appendSystemMessage("추천된 플랜이 없습니다.");
        } else {
            appendSystemMessage("추천 일정 " + entries.size() + "개가 도착했습니다. 마음에 드는 플랜을 선택해 보세요.");
            appendServerMessage("AI 추천 일정", buildPlanCardContainer(entries));
        }
        planListPanel.revalidate();
        planListPanel.repaint();
    }

    void updateFinalPlan(Map<?, ?> plan, Map<String, Object> rawResponse) {
        String summary = valueOrDefault(plan.get("summary"), "최종 일정");
        appendServerMessage(summary, buildFinalPlanComponent(plan));
        appendWeatherNotice(rawResponse == null ? null : rawResponse.get("weather"),
            valueOrDefault(rawResponse == null ? null : rawResponse.get("weather_notice"), ""));
        if (planHelperLabel != null) {
            planHelperLabel.setText("추가로 수정하고 싶다면 아래 입력창에 메시지를 보내보세요.");
        }
        if (detailControlPanel != null) {
            detailControlPanel.setVisible(true);
        }
        if (detailRequestButton != null) {
            detailRequestButton.setEnabled(true);
        }
        clearDetailInput();
        viewContext.ensureResultFrameVisible();
        viewContext.showPlansCard();
    }

    void setDetailControlsVisible(boolean visible) {
        if (detailControlPanel != null) {
            detailControlPanel.setVisible(visible);
        }
    }

    void setDetailRequestEnabled(boolean enabled) {
        if (detailRequestButton != null) {
            detailRequestButton.setEnabled(enabled);
        }
    }

    void clearDetailInput() {
        if (detailRequestField != null) {
            detailRequestField.setText("");
        }
    }

    void appendSystemMessage(String text) {
        appendChatBubble(false, "안내", createChatTextArea(text), systemChatColor);
    }

    void appendUserMessage(String text) {
        String display = (text == null || text.isBlank())
            ? "선택한 플랜으로 세부 일정을 다시 만들어줘."
            : text;
        appendChatBubble(true, "사용자", createChatTextArea(display), userChatColor);
    }

    void appendServerMessage(String title, JComponent content) {
        JComponent body = content == null ? createChatTextArea("") : content;
        body.setOpaque(false);
        appendChatBubble(false, title, body, serverChatColor);
    }

    private void notifyDetailRequest() {
        if (listener == null || detailRequestField == null) {
            return;
        }
        String userRequest = detailRequestField.getText() == null ? "" : detailRequestField.getText().trim();
        listener.onDetailRequestSubmitted(userRequest);
    }

    private JPanel buildPlanCardContainer(List<PlanEntry> entries) {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        int index = 1;
        for (PlanEntry entry : entries) {
            container.add(createPlanCard(index++, entry));
            container.add(Box.createVerticalStrut(8));
        }
        return container;
    }

    private JPanel createPlanCard(int index, PlanEntry entry) {
        Map<?, ?> option = entry.data();
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(223, 227, 235)),
            new EmptyBorder(8, 12, 8, 12)
        ));
        String title = valueOrDefault(option.get("title"), "플랜 " + index);
        JLabel titleLabel = new JLabel(index + ". " + title);
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.WEST);
        String dayLabelText = entry.dayLabel();
        if (dayLabelText != null && !dayLabelText.isBlank()) {
            JLabel dayLabel = new JLabel(dayLabelText);
            dayLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 11));
            dayLabel.setForeground(new Color(120, 130, 149));
            header.add(dayLabel, BorderLayout.EAST);
        }
        card.add(header, BorderLayout.NORTH);

        card.add(createScheduleFlow(option.get("schedule")), BorderLayout.CENTER);

        String planId = valueOrDefault(option.get("id"), "");
        JButton selectButton = new JButton("AI 세부 플랜 생성");
        selectButton.setEnabled(!planId.isBlank());
        selectButton.addActionListener(e -> {
            if (listener != null) {
                listener.onPlanSelectionRequested(planId, title);
            }
        });
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(selectButton);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JComponent createScheduleFlow(Object scheduleObj) {
        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        flow.setOpaque(false);
        List<Map<?, ?>> entries = extractScheduleEntries(scheduleObj);
        if (entries.isEmpty()) {
            flow.add(new JLabel("일정 정보가 없습니다."));
            return flow;
        }
        for (Map<?, ?> entry : entries) {
            String time = valueOrDefault(entry.get("time"), "");
            String activity = valueOrDefault(entry.get("activity"), "");
            flow.add(createScheduleChip(time, activity));
        }
        return flow;
    }

    private JComponent createScheduleChip(String time, String activity) {
        JPanel chip = new JPanel();
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(208, 217, 232)),
            new EmptyBorder(6, 10, 6, 10)
        ));
        chip.setLayout(new BoxLayout(chip, BoxLayout.Y_AXIS));

        JLabel timeLabel = new JLabel(time.isBlank() ? "시간 미정" : time);
        timeLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 11));
        timeLabel.setForeground(new Color(76, 86, 106));
        chip.add(timeLabel);

        JLabel activityLabel = new JLabel(activity.isBlank() ? "활동 정보 없음" : activity);
        activityLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        chip.add(activityLabel);

        return chip;
    }

    private List<Map<?, ?>> extractScheduleEntries(Object scheduleObj) {
        List<Map<?, ?>> result = new ArrayList<>();
        if (scheduleObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add(map);
                }
            }
        } else if (scheduleObj instanceof Map<?, ?> map) {
            result.add(map);
        }
        return result;
    }

    private JComponent buildFinalPlanComponent(Map<?, ?> plan) {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        Object daysObj = plan.get("days");
        if (daysObj instanceof List<?> list) {
            int index = 1;
            for (Object day : list) {
                if (day instanceof Map<?, ?> map) {
                    container.add(createFinalDayCard(index++, map));
                    container.add(Box.createVerticalStrut(6));
                }
            }
        } else {
            container.add(createChatTextArea("세부 일정 정보를 찾지 못했습니다."));
        }
        return container;
    }

    private Component createFinalDayCard(int index, Map<?, ?> dayMap) {
        JPanel card = new JPanel(new BorderLayout());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(223, 227, 235)),
            new EmptyBorder(6, 10, 6, 10)
        ));
        Object titleObj = dayMap.get("title");
        if (titleObj == null) {
            titleObj = dayMap.get("day");
        }
        String title = valueOrDefault(titleObj, "Day " + index);
        JLabel header = new JLabel(title);
        header.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        card.add(header, BorderLayout.NORTH);

        JTextArea area = new JTextArea();
        area.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setText(buildScheduleText(dayMap.get("schedule")));
        area.setOpaque(false);
        card.add(area, BorderLayout.CENTER);
        return card;
    }

    private String buildScheduleText(Object scheduleObj) {
        if (scheduleObj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object time = map.get("time");
                    Object activity = map.get("activity");
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(valueOrDefault(time, "")).append(" - ").append(valueOrDefault(activity, ""));
                } else {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(String.valueOf(item));
                }
            }
            return sb.toString();
        }
        if (scheduleObj instanceof Map<?, ?> map) {
            return map.toString();
        }
        return scheduleObj == null ? "" : scheduleObj.toString();
    }

    private void appendWeatherNotice(Object weatherNode, String noticeText) {
        boolean hasWeather = weatherNode instanceof Map<?, ?>;
        if (!hasWeather && (noticeText == null || noticeText.isBlank())) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (hasWeather) {
            Map<?, ?> weather = (Map<?, ?>) weatherNode;
            String description = valueOrDefault(weather.get("description"), valueOrDefault(weather.get("condition"), ""));
            String temp = valueOrDefault(weather.get("temperature"), "");
            if (!description.isBlank()) {
                sb.append("날씨: ").append(description);
            }
            if (!temp.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("기온: ").append(temp).append("℃");
            }
            Object humidity = weather.get("humidity");
            if (humidity != null) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("습도 ").append(humidity).append("%");
            }
        }
        if (noticeText != null && !noticeText.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(noticeText);
        }
        if (sb.length() == 0) {
            return;
        }
        appendServerMessage("날씨 안내", createChatTextArea(sb.toString()));
    }

    private List<PlanEntry> extractPlanEntries(Object node, String currentDayLabel) {
        List<PlanEntry> entries = new ArrayList<>();
        collectPlanEntries(node, currentDayLabel, 0, entries);
        return entries;
    }

    private void collectPlanEntries(Object node, String currentDayLabel, int currentDayNumber, List<PlanEntry> entries) {
        if (node == null) {
            return;
        }
        if (node instanceof Map<?, ?> map) {
            if (looksLikePlan(map)) {
                entries.add(new PlanEntry(map, currentDayLabel, currentDayNumber));
                return;
            }

            String nextDayLabel = deriveDayLabel(map, currentDayLabel);
            int nextDayNumber = normalizeDayNumber(map.get("day"), nextDayLabel);

            Object daysNode = map.get("days");
            if (daysNode != null) {
                collectPlanEntries(daysNode, nextDayLabel, nextDayNumber, entries);
            }

            Object plansNode = map.get("plans");
            if (plansNode != null && plansNode != node) {
                collectPlanEntries(plansNode, nextDayLabel, nextDayNumber, entries);
            }

            Object optionsNode = map.get("options");
            if (optionsNode != null) {
                collectPlanEntries(optionsNode, nextDayLabel, nextDayNumber, entries);
            }

            Object variantsNode = map.get("variants");
            if (variantsNode != null) {
                collectPlanEntries(variantsNode, nextDayLabel, nextDayNumber, entries);
            }
            return;
        }

        if (node instanceof List<?> list) {
            for (Object item : list) {
                collectPlanEntries(item, currentDayLabel, currentDayNumber, entries);
            }
        }
    }

    private String deriveDayLabel(Map<?, ?> map, String fallback) {
        String label = valueOrDefault(map.get("title"), fallback);
        if ((label == null || label.isBlank()) && map.get("day") != null) {
            label = "Day " + map.get("day");
        }
        return label == null ? "" : label;
    }

    private boolean looksLikePlan(Map<?, ?> map) {
        boolean hasSchedule = map.containsKey("schedule") || map.containsKey("activities");
        boolean hasTitle = map.containsKey("title") || map.containsKey("name");
        boolean hasId = map.containsKey("id") || map.containsKey("plan_id");
        return hasSchedule && (hasTitle || hasId);
    }

    private int normalizeDayNumber(Object dayValue, String dayLabel) {
        if (dayValue instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        String candidate = null;
        if (dayValue != null) {
            candidate = dayValue.toString();
        } else if (dayLabel != null) {
            candidate = dayLabel;
        }
        if (candidate != null) {
            String digits = candidate.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    return Integer.parseInt(digits);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private void appendChatBubble(boolean alignRight, String title, JComponent body, Color bubbleColor) {
        if (planListPanel == null) {
            return;
        }
        JPanel wrapper = new JPanel(new FlowLayout(alignRight ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 6));
        wrapper.setOpaque(false);
        JPanel bubble = new JPanel(new BorderLayout(0, 4));
        bubble.setOpaque(true);
        bubble.setBackground(bubbleColor);
        bubble.setBorder(new EmptyBorder(10, 12, 10, 12));
        if (title != null && !title.isBlank()) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
            bubble.add(titleLabel, BorderLayout.NORTH);
        }
        if (body == null) {
            body = createChatTextArea("");
        }
        bubble.add(body, BorderLayout.CENTER);
        wrapper.add(bubble);
        planListPanel.add(wrapper);
        planListPanel.revalidate();
        planListPanel.repaint();
        scrollChatToBottom();
    }

    private JTextArea createChatTextArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder());
        return area;
    }

    private void scrollChatToBottom() {
        if (planScrollPane == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = planScrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    static String valueOrDefault(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}

