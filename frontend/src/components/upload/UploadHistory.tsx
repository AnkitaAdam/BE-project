import { useEffect, useState } from "react";
import { getUploadHistory } from "../../api/logApi";
import { List, ListItem, Typography } from "@mui/material";

export default function UploadHistory() {
  const [history, setHistory] = useState<any[]>([]);

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    const res = await getUploadHistory();
    setHistory(res.data);
  };

  return (
    <>
      <Typography variant="h6">Upload History</Typography>
      <List>
        {history.map((item, index) => (
          <ListItem key={index}>{item.fileName}</ListItem>
        ))}
      </List>
    </>
  );
}