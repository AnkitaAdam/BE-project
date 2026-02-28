// import { Container, Grid, Typography, Paper } from "@mui/material";
// import UploadForm from "../components/upload/UploadForm";
// import UploadHistory from "../components/upload/UploadHistory";

// export default function Dashboard() {
//   return (
//     <Container maxWidth="lg">
//       <Typography variant="h4" sx={{ mb: 3 }}>
//         Vulnuris Log Intelligence Platform
//       </Typography>

//       <Grid container spacing={3}>
//         <Grid item xs={12} md={6}>
//           <Paper sx={{ p: 3 }}>
//             <UploadForm />
//           </Paper>
//         </Grid>

//         <Grid item xs={12} md={6}>
//           <Paper sx={{ p: 3 }}>
//             <UploadHistory />
//           </Paper>
//         </Grid>
//       </Grid>
//     </Container>
//   );
// }

import Layout from "../components/common/layout";
import UploadCard from "../components/upload/UploadCard";
import HistoryTable from "../components/upload/HistoryTable";
import { Grid, Typography } from "@mui/material";


export default function Dashboard() {
  return (
    <Layout>
      <Typography variant="h4" sx={{ mb: 4 }}>
        Security Intelligence Dashboard
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <UploadCard />
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <HistoryTable />
        </Grid>
      </Grid>
    </Layout>
  );
}