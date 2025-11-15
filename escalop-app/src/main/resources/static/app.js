const $ = (id) => document.getElementById(id);

const primaryColorInput = $("primaryColor");
const secondaryColorInput = $("secondaryColor");
const pointStyleSelect = $("pointStyle");
const pointsCountInput = $("pointsCount");
const seriesListInput = $("seriesList");
const reloadBtn = $("reloadBtn");

let chart;

/** Применяем выбранные цвета в CSS-переменные (для кнопок/фона и т.п.) */
function applyTheme() {
    document.documentElement.style.setProperty("--accent-1", primaryColorInput.value);
    document.documentElement.style.setProperty("--accent-2", secondaryColorInput.value);
}

/** Загружаем данные с бэка */
async function loadData(points = 80, series = [1, 2]) {
    const qs = new URLSearchParams();
    qs.set("n", String(points));
    series.forEach(s => qs.append("series", String(s)));
    const res = await fetch(`/api/series?${qs.toString()}`);
    if (!res.ok) {
        throw new Error(`API error: ${res.status} ${await res.text()}`);
    }
    return res.json();
}

/** Строим конфиг Chart.js с настраиваемыми цветами/формами */
function toChartConfig(payload, colors, pointStyle) {
    const palette = [
        colors.primary,
        colors.secondary,
        "#e15759",
        "#76b7b2",
        "#59a14f",
        "#edc948",
        "#b07aa1",
        "#ff9da7",
    ];

    return {
        type: "line",
        data: {
            labels: payload.labels,
            datasets: payload.datasets.map((ds, i) => ({
                label: ds.label,
                data: ds.data,
                borderColor: palette[i % palette.length],
                backgroundColor: palette[i % palette.length] + "33",
                tension: 0.25,
                pointRadius: 3,
                pointHoverRadius: 5,
                pointStyle: ds.shape ?? pointStyle,
                fill: false
            }))
        },
        options: {
            responsive: true,
            animation: false,
            scales: {
                x: { grid: { color: "#2a2f3a" } },
                y: { grid: { color: "#2a2f3a" } }
            },
            plugins: {
                legend: { labels: { color: "#cfd6e6" } },
                tooltip: {
                    mode: "nearest",
                    intersect: false
                }
            },
            elements: {
                line: { borderWidth: 2 }
            }
        }
    };
}

async function buildChart() {
    applyTheme();

    const n = Number(pointsCountInput.value || 80);
    const series = (seriesListInput.value || "1,2")
        .split(",")
        .map(s => Number(s.trim()))
        .filter(n => !Number.isNaN(n) && n > 0);

    const payload = await loadData(n, series);

    const ctx = document.getElementById("mainChart").getContext("2d");
    const cfg = toChartConfig(
        payload,
        { primary: primaryColorInput.value, secondary: secondaryColorInput.value },
        pointStyleSelect.value
    );

    if (chart) chart.destroy();
    chart = new Chart(ctx, cfg);
}

reloadBtn.addEventListener("click", () => {
    buildChart().catch(err => alert(err.message));
});

// Первичная инициализация
buildChart().catch(err => alert(err.message));
