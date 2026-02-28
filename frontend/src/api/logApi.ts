import axios from "axios";

const API = axios.create({
  baseURL: "http://localhost:8080",
});

export const uploadLogs = (file: File) => {
  const formData = new FormData();
  formData.append("file", file);

  return API.post("/logs/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
};

export const getUploadHistory = () => {
  return API.get("/logs");
};