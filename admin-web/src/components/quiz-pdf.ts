import type { Content } from "../domain/content";
import type { Quiz, QuizVersion } from "../domain/quiz";
import type { MediaApi } from "../api/media-api";
import type { Content as PdfContent, TDocumentDefinitions } from "pdfmake/interfaces";
import { translate } from "../i18n/I18nContext";
import type { Language } from "../i18n/types";

export interface QuizPdfModel {
  fileName: string;
  title: string;
  metadataLines: string[];
  description: string;
  questions: Array<{
    heading: string;
    progress: string;
    prompt: string;
    options: Array<{ label: string; correct: boolean }>;
    imageContentUrl: string | null;
    imageAlternativeText: string | null;
    accessiblePrompt: string | null;
  }>;
}

export function buildQuizPdfModel(content: Content, quiz: Quiz, version: QuizVersion, quizNumber: number, language: Language = "tr"): QuizPdfModel {
  const scope = translate(language, quiz.scopeType === "CONTENT" ? "viewer.generalContent" : quiz.scopeType === "SEASON" ? "pdf.scopeSeason" : "pdf.scopeEpisode");
  return {
    fileName: safeFileName(`${content.title}-quiz-${quizNumber}-v${version.versionNumber}.pdf`),
    title: version.title,
    metadataLines: [
      `${translate(language, content.contentType === "SERIES" ? "common.series" : "common.film")}: ${content.title}`,
      translate(language, "pdf.quizMetadata", { quiz: quizNumber, version: version.versionNumber, status: version.status }),
      translate(language, "pdf.scopeMetadata", { scope, count: version.questions.length })
    ],
    description: version.description?.trim() || translate(language, "pdf.noDescription"),
    questions: version.questions.map((question, index) => ({
      heading: translate(language, "admin.questionNumber", { number: question.questionOrder }),
      progress: `${index + 1} / ${version.questions.length}`,
      prompt: question.prompt,
      options: question.answerOptions.map((option) => ({ label: `${option.optionOrder}. ${option.text}`, correct: option.correct })),
      imageContentUrl: question.visualMediaId
        ? `/api/v1/media/${question.visualMediaId}/content`
        : content.coverImageUrl,
      imageAlternativeText: question.visualMediaId
        ? question.visualAlternativeText
        : content.coverAlternativeText,
      accessiblePrompt: question.accessiblePrompt
    }))
  };
}

export async function downloadQuizPdf(content: Content, quiz: Quiz, version: QuizVersion, quizNumber: number, mediaApi: MediaApi, language: Language = "tr") {
  const model = buildQuizPdfModel(content, quiz, version, quizNumber, language);
  const imageDataByUrl = await loadPdfImages(model, mediaApi);
  const [{ default: pdfMake }, { default: vfs }] = await Promise.all([
    import("pdfmake/build/pdfmake"),
    import("pdfmake/build/vfs_fonts")
  ]);
  pdfMake.addVirtualFileSystem(vfs);
  const documentContent: PdfContent[] = [
    { text: translate(language, "pdf.studio"), color: "#177a60", bold: true, fontSize: 10, characterSpacing: 1.5 },
    { text: model.title, fontSize: 26, bold: true, color: "#17201d", margin: [0, 10, 0, 10] }
  ];
  model.metadataLines.forEach((line) => documentContent.push({ text: line, fontSize: 10, color: "#50615b", margin: [0, 2, 0, 0] }));
  documentContent.push({ text: model.description, fontSize: 11, color: "#26332f", margin: [0, 12, 0, 18] });
  model.questions.forEach((question) => {
    const imageData = question.imageContentUrl ? imageDataByUrl.get(question.imageContentUrl) : null;
    documentContent.push(
      { text: "", pageBreak: "before" },
      {
        columns: [
          { text: question.heading, fontSize: 11, bold: true, color: "#177a60" },
          { text: question.progress, alignment: "right", fontSize: 10, color: "#50615b" },
          { text: "30 sn", alignment: "right", width: 44, bold: true, fontSize: 10, color: "#ffffff", fillColor: "#177a60", margin: [8, 4, 8, 4] }
        ],
        margin: [0, 0, 0, 14]
      }
    );
    if (imageData) {
      documentContent.push({ image: imageData, fit: [500, 250], alignment: "center", margin: [0, 0, 0, 14] });
    }
    documentContent.push(
      { text: question.prompt, fontSize: 18, bold: true, color: "#17201d", margin: [0, 0, 0, 14] },
      ...question.options.map((option) => ({
        table: { widths: ["*"], body: [[{ text: `${option.correct ? "✓  " : ""}${option.label}`, bold: option.correct, color: option.correct ? "#0d654f" : "#26332f", fillColor: option.correct ? "#dff5eb" : "#f4f7f6", margin: [12, 10, 12, 10] }]] },
        layout: { hLineColor: () => option.correct ? "#42a785" : "#d8e1de", vLineColor: () => option.correct ? "#42a785" : "#d8e1de" },
        margin: [0, 0, 0, 8]
      })) as PdfContent[]
    );
    if (question.imageAlternativeText) documentContent.push({ text: translate(language, "pdf.imageDescription", { text: question.imageAlternativeText }), italics: true, fontSize: 9, color: "#687771", margin: [0, 6, 0, 2] });
    if (question.accessiblePrompt) documentContent.push({ text: translate(language, "pdf.accessibleQuestion", { text: question.accessiblePrompt }), italics: true, fontSize: 9, color: "#687771" });
  });
  const definition: TDocumentDefinitions = {
    info: { title: model.title, subject: translate(language, "pdf.subject", { title: content.title }), creator: translate(language, "admin.brand") },
    pageMargins: [44, 46, 44, 46],
    content: documentContent,
    footer: (currentPage, pageCount) => ({ text: `${currentPage} / ${pageCount}`, alignment: "center", fontSize: 8, color: "#7b8984" }),
    defaultStyle: { font: "Roboto" }
  };
  pdfMake.createPdf(definition).download(model.fileName);
}

async function loadPdfImages(model: QuizPdfModel, mediaApi: MediaApi): Promise<Map<string, string>> {
  const uniqueImageUrls = [...new Set(model.questions.map((question) => question.imageContentUrl).filter((url): url is string => Boolean(url)))];
  const loadedImages = await Promise.all(uniqueImageUrls.map(async (contentUrl) => {
    try {
      return [contentUrl, await mediaApi.loadImageDataUrl(contentUrl)] as const;
    } catch {
      return [contentUrl, null] as const;
    }
  }));
  return new Map(loadedImages.filter((entry): entry is readonly [string, string] => entry[1] !== null));
}

function safeFileName(value: string) {
  return value.normalize("NFKD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-zA-Z0-9._-]+/g, "-").replace(/-+/g, "-").toLowerCase();
}
