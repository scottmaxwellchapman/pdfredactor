<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PDF Redactor Web</title>
    <link rel="stylesheet" href="static/app.css">
</head>
<body>
<div class="container">
    <header>
        <h1>PDF Redactor <span>web</span></h1>
        <p>Scott Maxwell Chapman · pdfredactor@chapmanlaw.group</p>
    </header>

    <section class="panel upload-panel">
        <form id="uploadForm">
            <label>Quality (0.1 - 1.0):
                <input type="number" id="quality" name="quality" min="0.1" max="1.0" step="0.1" value="0.5" required>
            </label>
            <input type="file" id="pdf" name="pdf" accept="application/pdf" required>
            <button type="submit">Select a PDF to Redact</button>
        </form>
        <p id="status">Upload a PDF to begin.</p>
    </section>

    <section class="panel viewer-panel">
        <div class="controls">
            <button id="prevBtn">Previous</button>
            <span id="pageLabel">Page 0 of 0</span>
            <button id="nextBtn">Next</button>
            <label>Go to page:
                <input id="jumpPage" type="number" min="1" value="1">
            </label>
            <button id="jumpBtn">Go</button>
            <button id="undoBtn">Undo Redactions for this Page</button>
            <button id="downloadBtn">Finish (Download Redacted PDF)</button>
        </div>

        <div id="canvasWrapper">
            <canvas id="pdfCanvas"></canvas>
        </div>
        <p id="coords">X: , Y: </p>
        <p class="steps">Steps: 1) Click top left of redaction area. 2) Click bottom right of redaction area.</p>
    </section>
</div>
<script src="static/app.js"></script>
</body>
</html>
