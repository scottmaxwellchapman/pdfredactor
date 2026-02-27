PDF Redactor is an open source Java application that helps legal professionals redact PDFs.

Developed by Scott Maxwell Chapman

pdfredactor@chapmanlaw.group

Home Page: https://www.chapmanlaw.group/wordpress/index.php/pdf-redactor/


Demo Video: https://www.youtube.com/watch?v=3-Cps6Ynl6Q

## Headless and Embedded Usage

You can now run the app without launching Swing UI:

- Print per-page dimensions in pixels:
  - `java -cp target/pdfredactor-1.0-SNAPSHOT.jar group.chapmanlaw.pdfredactor.app dimensions input.pdf`
  - Optional quality argument: `quality=0.1-1.0`
  - Output format is CSV per line: `pageNumber,widthPx,heightPx`

- Apply per-page redactions in pixels and save to a new PDF:
  - `java -cp target/pdfredactor-1.0-SNAPSHOT.jar group.chapmanlaw.pdfredactor.app redact input.pdf output.pdf 1:100,100,80,60 2:200,300,120,40 quality=1.0`
  - Redaction tokens use `page:x,y,width,height` where page numbering is 1-based.

For embedded integrations, call `group.chapmanlaw.pdfredactor.headless` directly:

- `openPdf(String inputPdfPath, float quality)`
- `getPageDimensionsInPixels()`
- `applyPixelRedactions(Map<Integer, List<Rectangle>> redactionsByPage, String outputPdfPath)`

`redactionsByPage` uses zero-based page indexes with rectangle values in rendered pixel coordinates.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
