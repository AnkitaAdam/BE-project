import { Paper, Typography, Table, TableRow, TableCell, TableHead, TableBody } from "@mui/material";

export default function HistoryTable() {
  const data = [
    { file: "aws_logs.json", status: "Processed", time: "2 mins ago" },
  ];

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6">Recent Uploads</Typography>

      <Table>
        <TableHead>
          <TableRow>
            <TableCell>File</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Time</TableCell>
          </TableRow>
        </TableHead>

        <TableBody>
          {data.map((row, i) => (
            <TableRow key={i}>
              <TableCell>{row.file}</TableCell>
              <TableCell>{row.status}</TableCell>
              <TableCell>{row.time}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
}