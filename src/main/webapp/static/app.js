const uploadForm = document.getElementById('uploadForm');
const statusEl = document.getElementById('status');
const canvas = document.getElementById('pdfCanvas');
const ctx = canvas.getContext('2d');
const coordsLabel = document.getElementById('coords');
const pageLabel = document.getElementById('pageLabel');
const prevBtn = document.getElementById('prevBtn');
const nextBtn = document.getElementById('nextBtn');
const jumpBtn = document.getElementById('jumpBtn');
const jumpPage = document.getElementById('jumpPage');
const undoBtn = document.getElementById('undoBtn');
const downloadBtn = document.getElementById('downloadBtn');

let totalPages = 0;
let currentPage = 0;
let currentImage = null;
let firstClick = null;

async function api(path, options = {}) {
    const response = await fetch('/api' + path, options);
    if (!response.ok) {
        let message = 'Request failed.';
        try {
            const data = await response.json();
            if (data.error) {
                message = data.error;
            }
        } catch (err) {
            // ignore JSON parse errors
        }
        throw new Error(message);
    }
    return response;
}

async function refreshStatus() {
    try {
        const statusResponse = await api('/status');
        const payload = await statusResponse.json();
        totalPages = payload.loaded ? payload.totalPages : 0;
        if (!payload.loaded) {
            statusEl.textContent = 'Upload a PDF to begin.';
            pageLabel.textContent = 'Page 0 of 0';
            return;
        }

        statusEl.textContent = 'PDF loaded successfully.';
        currentPage = 0;
        jumpPage.max = totalPages;
        jumpPage.value = 1;
        await loadPage(currentPage);
    } catch (error) {
        statusEl.textContent = error.message;
    }
}

async function loadPage(pageIndex) {
    const imageResponse = await api(`/page?page=${pageIndex}`);
    const blob = await imageResponse.blob();
    const imageBitmap = await createImageBitmap(blob);
    currentImage = imageBitmap;

    canvas.width = imageBitmap.width;
    canvas.height = imageBitmap.height;
    ctx.drawImage(imageBitmap, 0, 0);

    pageLabel.textContent = `Page ${pageIndex + 1} of ${totalPages}`;
    prevBtn.disabled = pageIndex <= 0;
    nextBtn.disabled = pageIndex >= totalPages - 1;
}

function redrawSelection(currentPoint = null) {
    if (!currentImage) return;
    ctx.drawImage(currentImage, 0, 0);
    const activePoint = currentPoint || firstClick;
    if (!firstClick || !activePoint) return;

    const x = Math.min(firstClick.x, activePoint.x);
    const y = Math.min(firstClick.y, activePoint.y);
    const width = Math.abs(firstClick.x - activePoint.x);
    const height = Math.abs(firstClick.y - activePoint.y);

    ctx.strokeStyle = '#ff3b3b';
    ctx.lineWidth = 2;
    ctx.strokeRect(x, y, width, height);
}

canvas.addEventListener('mousemove', event => {
    const rect = canvas.getBoundingClientRect();
    const x = Math.round((event.clientX - rect.left) * (canvas.width / rect.width));
    const y = Math.round((event.clientY - rect.top) * (canvas.height / rect.height));
    coordsLabel.textContent = `X: ${x}, Y: ${y}`;

    if (firstClick) {
        redrawSelection({ x, y });
    }
});

canvas.addEventListener('click', async event => {
    if (!totalPages) return;

    const rect = canvas.getBoundingClientRect();
    const x = Math.round((event.clientX - rect.left) * (canvas.width / rect.width));
    const y = Math.round((event.clientY - rect.top) * (canvas.height / rect.height));

    if (!firstClick) {
        firstClick = { x, y };
        redrawSelection({ x, y });
        return;
    }

    const secondClick = { x, y };
    if (firstClick.x === secondClick.x || firstClick.y === secondClick.y) {
        statusEl.textContent = 'Please click two different corners to define a redaction area.';
        firstClick = null;
        redrawSelection();
        return;
    }

    const formData = new URLSearchParams({
        page: currentPage.toString(),
        x1: firstClick.x.toString(),
        y1: firstClick.y.toString(),
        x2: secondClick.x.toString(),
        y2: secondClick.y.toString()
    });

    try {
        await api('/redact', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        });

        firstClick = null;
        await loadPage(currentPage);
        statusEl.textContent = 'Redaction applied.';
    } catch (error) {
        statusEl.textContent = error.message;
        firstClick = null;
    }
});

uploadForm.addEventListener('submit', async event => {
    event.preventDefault();
    const formData = new FormData(uploadForm);
    try {
        statusEl.textContent = 'Uploading and rendering pages...';
        await api('/upload', { method: 'POST', body: formData });
        await refreshStatus();
    } catch (error) {
        statusEl.textContent = error.message;
    }
});

prevBtn.addEventListener('click', async () => {
    if (currentPage > 0) {
        currentPage -= 1;
        firstClick = null;
        await loadPage(currentPage);
    }
});

nextBtn.addEventListener('click', async () => {
    if (currentPage < totalPages - 1) {
        currentPage += 1;
        firstClick = null;
        await loadPage(currentPage);
    }
});

jumpBtn.addEventListener('click', async () => {
    const requestedPage = Number(jumpPage.value) - 1;
    if (requestedPage >= 0 && requestedPage < totalPages) {
        currentPage = requestedPage;
        firstClick = null;
        await loadPage(currentPage);
    }
});

undoBtn.addEventListener('click', async () => {
    try {
        const formData = new URLSearchParams({ page: currentPage.toString() });
        await api('/undo', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        });
        await loadPage(currentPage);
        statusEl.textContent = 'Undo complete for current page.';
    } catch (error) {
        statusEl.textContent = error.message;
    }
});

downloadBtn.addEventListener('click', () => {
    window.location.href = '/api/download';
});

window.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
        firstClick = null;
        redrawSelection();
        coordsLabel.textContent = 'X: , Y: ';
    }
});

refreshStatus();
