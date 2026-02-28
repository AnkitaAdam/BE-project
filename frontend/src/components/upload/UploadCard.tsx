import { Paper, Typography, Button, Box } from "@mui/material";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import { useState } from "react";
import { uploadLogs } from "../../api/logApi";

export default function UploadCard() {
  const [file, setFile] = useState<File | null>(null);

  const handleUpload = async () => {
    if (!file) return;
    await uploadLogs(file);
    alert("Upload Successful");
  };

  return (
    <Paper
      sx={{
        p: 4,
        height: 250,
        background:
          "linear-gradient(135deg, rgba(59,130,246,0.2), rgba(6,182,212,0.2))",
        backdropFilter: "blur(10px)",
      }}
    >
      <Typography variant="h6">Upload Security Logs</Typography>

      <Box
        sx={{
          mt: 2,
          border: "2px dashed #3B82F6",
          borderRadius: 3,
          height: 120,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          cursor: "pointer",
        }}
        onClick={() => document.getElementById("file")?.click()}
      >
        <CloudUploadIcon sx={{ mr: 2 }} />
        {file ? file.name : "Drag & Drop or Click to Upload"}
      </Box>

      <input
        id="file"
        type="file"
        hidden
        onChange={(e: any) => setFile(e.target.files[0])}
      />

      <Button
        variant="contained"
        sx={{ mt: 2 }}
        startIcon={<CloudUploadIcon />}
        onClick={handleUpload}
      >
        Upload Logs
      </Button>
    </Paper>
  );
}