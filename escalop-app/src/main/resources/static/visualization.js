const $ = (id) => document.getElementById(id);

const uploadsListEl = $("uploadsList");
const prevBtn = $("prevPage");
const nextBtn = $("nextPage");
const pageInfoEl = $("pageInfo");

const fileInput = $("fileInput");
const uploadButton = $("uploadButton");
const uploadStatusEl = $("uploadStatus");

const PAGE_SIZE = 5;
let historyData = [];
let currentPage = 0;

// --- Загрузка истории ---

async function loadUploadHistory() {
    console.log("Requesting /api/uploads");
    const res = await fetch("/api/uploads");
    if (!res.ok) {
        throw new Error(`Ошибка при загрузке истории: ${res.status}`);
    }
    historyData = await res.json();
    currentPage = 0;
    renderPage();
}

function renderPage() {
    if (!uploadsListEl) return;

    uploadsListEl.innerHTML = "";

    if (!historyData || historyData.length === 0) {
        uploadsListEl.textContent = "История загрузок пуста.";
        if (pageInfoEl) pageInfoEl.textContent = "";
        if (prevBtn) prevBtn.disabled = true;
        if (nextBtn) nextBtn.disabled = true;
        return;
    }

    const totalPages = Math.ceil(historyData.length / PAGE_SIZE);
    if (currentPage >= totalPages) currentPage = totalPages - 1;
    if (currentPage < 0) currentPage = 0;

    const start = currentPage * PAGE_SIZE;
    const end = start + PAGE_SIZE;
    const pageItems = historyData.slice(start, end);

    const list = document.createElement("div");
    list.style.display = "flex";
    list.style.flexDirection = "column";
    list.style.gap = "8px";

    pageItems.forEach((item) => {
        const row = document.createElement("div");
        row.style.display = "flex";
        row.style.justifyContent = "space-between";
        row.style.alignItems = "center";
        row.style.padding = "6px 8px";
        row.style.borderRadius = "4px";
        row.style.background = "rgba(255, 255, 255, 0.03)";

        const left = document.createElement("div");
        left.innerHTML = `
            <div><strong>${item.fileName}</strong></div>
            <div style="font-size: 0.9em; opacity: 0.8;">
                Формат: ${item.format || "неизвестен"}
                &nbsp;•&nbsp;
                Тип: ${item.analysisType || "-"}
            </div>
        `;

        const right = document.createElement("div");
        right.style.fontSize = "0.9em";
        right.style.opacity = "0.8";
        right.textContent = item.date || "";

        row.appendChild(left);
        row.appendChild(right);
        list.appendChild(row);
    });

    uploadsListEl.appendChild(list);

    if (pageInfoEl) {
        pageInfoEl.textContent = `Страница ${currentPage + 1} из ${totalPages}`;
    }
    if (prevBtn) prevBtn.disabled = currentPage === 0;
    if (nextBtn) nextBtn.disabled = currentPage >= totalPages - 1;
}

// --- Пагинация ---

function safeAddListener(el, event, handler) {
    if (!el) return;
    el.addEventListener(event, handler);
}

safeAddListener(prevBtn, "click", () => {
    if (currentPage > 0) {
        currentPage -= 1;
        renderPage();
    }
});

safeAddListener(nextBtn, "click", () => {
    const totalPages = Math.ceil(historyData.length / PAGE_SIZE);
    if (currentPage < totalPages - 1) {
        currentPage += 1;
        renderPage();
    }
});

// --- Загрузка нового файла ---

async function uploadFile() {
    const file = fileInput?.files?.[0];
    if (!file) {
        if (uploadStatusEl) {
            uploadStatusEl.style.color = "darkred";
            uploadStatusEl.textContent = "Сначала выберите файл.";
        }
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    if (uploadStatusEl) {
        uploadStatusEl.style.color = "";
        uploadStatusEl.textContent = "Загрузка...";
    }

    try {
        const res = await fetch("/api/upload", {
            method: "POST",
            body: formData,
        });

        if (!res.ok) {
            const text = await res.text();
            throw new Error(text || `HTTP ${res.status}`);
        }

        const payload = await res.json(); // UploadDocumentResponse

        if (payload.status === 200) {
            if (uploadStatusEl) {
                uploadStatusEl.style.color = "darkgreen";
                uploadStatusEl.textContent = "Файл успешно загружен.";
            }
            if (fileInput) fileInput.value = "";
            await loadUploadHistory();
        } else {
            if (uploadStatusEl) {
                uploadStatusEl.style.color = "darkred";
                uploadStatusEl.textContent =
                    payload.error || "Не удалось обработать файл.";
            }
        }
    } catch (e) {
        console.error(e);
        if (uploadStatusEl) {
            uploadStatusEl.style.color = "darkred";
            uploadStatusEl.textContent = "Ошибка при загрузке файла.";
        }
    }
}

safeAddListener(uploadButton, "click", () => {
    uploadFile().catch(console.error);
});

// --- Инициализация ---

function init() {
    console.log("init() called, requesting /api/uploads");
    loadUploadHistory().catch((err) => {
        console.error("loadUploadHistory error", err);
        if (uploadsListEl) {
            uploadsListEl.textContent = "Ошибка при загрузке истории.";
        }
    });
}

window.addEventListener("load", init);
console.log("visualization.js loaded");
