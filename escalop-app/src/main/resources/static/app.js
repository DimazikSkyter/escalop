const $ = (id) => document.getElementById(id);

const analysisTypeSelect = $("analysisTypeSelect");

let chart = null;
let rawPayload = null;

// Маппинг кодов анализов на русские названия
const ANALYSIS_LABELS = {
    BLOOD_GENERAL: "Общий анализ крови",
    BLOOD_CHEMISTIC: "Химия крови",
    ULTRASOUND: "УЗИ"
};

// --- Загрузка данных с бэка ---

async function loadData() {
    const res = await fetch("/api/data");
    if (!res.ok) {
        throw new Error(`API error: ${res.status} ${await res.text()}`);
    }
    return res.json(); // ожидаем GetDataResponse
}

// Заполнить список типов анализов
function initAnalysisTypeOptions(payload) {
    const types = Array.from(
        new Set(payload.results.map((r) => r.analysisType))
    ).sort();

    analysisTypeSelect.innerHTML = "";

    for (const t of types) {
        const opt = document.createElement("option");
        opt.value = t;
        opt.textContent = ANALYSIS_LABELS[t] ?? t;
        analysisTypeSelect.appendChild(opt);
    }
}

// --- Подготовка данных для Chart.js ---

function toChartConfig(payload, analysisType) {
    // Берём только нужный тип анализа и только те записи, у которых есть дата
    const docs = payload.results
        .filter((r) => r.analysisType === analysisType && r.date)
        .sort((a, b) => a.date.localeCompare(b.date)); // ISO-формат сортируется строкой

    // Если вдруг данных нет
    if (docs.length === 0) {
        return {
            type: "line",
            data: { datasets: [] },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    title: {
                        display: true,
                        text: "Нет данных для выбранного типа анализа",
                        color: "#ffffff"
                    }
                }
            }
        };
    }

    // Список всех метрик (имена уже приходят по-русски)
    const metricNames = Array.from(
        new Set(docs.flatMap((d) => d.metrics.map((m) => m.name)))
    );

    const palette = [
        "#4e79a7",
        "#f28e2b",
        "#e15759",
        "#76b7b2",
        "#59a14f",
        "#edc948",
        "#b07aa1",
        "#ff9da7"
    ];

    const datasets = metricNames.map((name, i) => ({
        label: name,
        data: docs.map((d) => {
            const metric = d.metrics.find((m) => m.name === name);
            return {
                x: d.date,                 // "YYYY-MM-DD"
                y: metric ? metric.value : null
            };
        }),
        borderColor: palette[i % palette.length],
        backgroundColor: palette[i % palette.length] + "33",
        tension: 0.25,
        pointRadius: 4,
        fill: false
    }));

    return {
        type: "line",
        data: { datasets },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    type: "time",
                    time: {
                        unit: "month",
                        tooltipFormat: "dd.MM.yyyy"
                    },
                    grid: { color: "#2a2f3a" },
                    ticks: { color: "#cfd6e6" },
                    title: {
                        display: true,
                        text: "Дата документа",
                        color: "#cfd6e6"
                    }
                },
                y: {
                    grid: { color: "#2a2f3a" },
                    ticks: { color: "#cfd6e6" },
                    title: {
                        display: true,
                        text: "Значение метрики",
                        color: "#cfd6e6"
                    }
                }
            },
            plugins: {
                legend: {
                    labels: { color: "#cfd6e6" }
                },
                tooltip: {
                    mode: "nearest",
                    intersect: false
                    // можно добавить кастомный title, но стандартного достаточно
                },
                title: {
                    display: true,
                    text: ANALYSIS_LABELS[analysisType] ?? analysisType,
                    color: "#ffffff",
                    font: { size: 18 }
                }
            }
        }
    };
}

// --- Построение и обновление графика ---

async function buildChart() {
    if (!rawPayload) {
        rawPayload = await loadData();
        initAnalysisTypeOptions(rawPayload);
    }

    const selectedType =
        analysisTypeSelect.value ||
        (analysisTypeSelect.options[0] && analysisTypeSelect.options[0].value);

    if (!selectedType) {
        return;
    }

    const ctx = document.getElementById("mainChart").getContext("2d");
    const cfg = toChartConfig(rawPayload, selectedType);

    if (chart) {
        chart.destroy();
    }
    chart = new Chart(ctx, cfg);
}

// Перестраиваем график по изменению селекта
analysisTypeSelect.addEventListener("change", () => {
    buildChart().catch((err) => alert(err.message));
});

// Первая отрисовка после загрузки страницы
document.addEventListener("DOMContentLoaded", () => {
    buildChart().catch((err) => alert(err.message));
});
