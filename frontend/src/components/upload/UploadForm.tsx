import { useState } from "react";
import {
  Button,
  Typography,
  LinearProgress,
  Box,
} from "@mui/material";
import { uploadLogs } from "../../api/logApi";

export default function UploadForm() {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);

  const handleFileChange = (e: any) => {
    setFile(e.target.files[0]);
  };

  const handleUpload = async () => {
    if (!file) return;

    setLoading(true);
    try {
      await uploadLogs(file);
      alert("Upload successful!");
    } catch (err) {
      alert("Upload failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Typography variant="h6">Upload Logs</Typography>

      <input type="file" onChange={handleFileChange} />

      <Button
        variant="contained"
        sx={{ mt: 2 }}
        onClick={handleUpload}
      >
        Upload
      </Button>

      {loading && <LinearProgress sx={{ mt: 2 }} />}
    </Box>
  );
}