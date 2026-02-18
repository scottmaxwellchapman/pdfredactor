# pdfredactor-web

`pdfredactor-web` is a fork of PDF Redactor that keeps the same workflow but moves the UI from Swing to a browser-based experience powered by embedded Tomcat + JSP.

## Features

- Upload a PDF in the browser.
- Redact pages by clicking two corners (top-left then bottom-right), matching the desktop flow.
- Navigate pages with Previous / Next / Go To.
- Undo page-level redactions.
- Download the final redacted PDF.
- Adjustable quality compression setting (0.1–1.0).

## Run

```bash
mvn compile exec:java
```

The app starts on port `8080`, or the next available port if 8080 is occupied.

If the environment is not headless, it attempts to open your default browser automatically.

## Original project

PDF Redactor is an open source Java application that helps legal professionals redact PDFs.

Developed by Scott Maxwell Chapman

pdfredactor@chapmanlaw.group

Home Page: https://www.chapmanlaw.group/wordpress/index.php/pdf-redactor/

Demo Video: https://www.youtube.com/watch?v=3-Cps6Ynl6Q
