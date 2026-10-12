"use strict";

// Additive, shared Blob read helper: a single GET returns image bytes and
// Content-Type. Existing authorization and privacy checks remain in handlers.
async function downloadBlobWithContentType(blobClient) {
  let response;
  try {
    response = await blobClient.download(0);
  } catch (error) {
    // Preserve the existing PHOTO_BLOB_NOT_FOUND response for absent objects.
    if (error && (error.statusCode === 404 || error.code === "BlobNotFound")) {
      return null;
    }
    throw error;
  }
  if (!response.readableStreamBody) {
    throw new Error("BLOB_DOWNLOAD_BODY_MISSING");
  }
  const chunks = [];
  for await (const chunk of response.readableStreamBody) {
    chunks.push(Buffer.from(chunk));
  }
  return {
    bytes: Buffer.concat(chunks),
    contentType: response.contentType
  };
}

module.exports = { downloadBlobWithContentType };
