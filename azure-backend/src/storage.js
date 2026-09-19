const { DefaultAzureCredential } = require("@azure/identity");
const { BlobServiceClient } = require("@azure/storage-blob");

let serviceClient;

function getRequiredEnv(name) {
  const value = process.env[name];
  if (!value) throw new Error(`Missing required environment variable: ${name}`);
  return value;
}

function getBlobServiceClient() {
  if (!serviceClient) {
    const accountUrl = getRequiredEnv("AZURE_STORAGE_ACCOUNT_URL");
    serviceClient = new BlobServiceClient(accountUrl, new DefaultAzureCredential());
  }
  return serviceClient;
}

function getContainer(name) {
  return getBlobServiceClient().getContainerClient(name);
}

function getProfilePhotosContainer() {
  return getContainer(process.env.AZURE_PROFILE_PHOTOS_CONTAINER || "profile-photos");
}

function getVerificationDocumentsContainer() {
  return getContainer(process.env.AZURE_VERIFICATION_DOCUMENTS_CONTAINER || "verification-documents");
}

async function checkBlobStorage() {
  const profilePhotos = getProfilePhotosContainer();
  const verificationDocuments = getVerificationDocumentsContainer();
  return {
    accountUrl: getRequiredEnv("AZURE_STORAGE_ACCOUNT_URL"),
    profilePhotosContainer: profilePhotos.containerName,
    profilePhotosExists: await profilePhotos.exists(),
    verificationDocumentsContainer: verificationDocuments.containerName,
    verificationDocumentsExists: await verificationDocuments.exists()
  };
}

module.exports = {
  getBlobServiceClient,
  getContainer,
  getProfilePhotosContainer,
  getVerificationDocumentsContainer,
  checkBlobStorage
};
