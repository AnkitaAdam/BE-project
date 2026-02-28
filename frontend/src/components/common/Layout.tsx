import {
  Box,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  Typography,
} from "@mui/material";
import DashboardIcon from "@mui/icons-material/Dashboard";

export default function Layout({ children }: any) {
  return (
    <Box sx={{ display: "flex" }}>
      <Drawer
        variant="permanent"
        sx={{
          width: 240,
          "& .MuiDrawer-paper": {
            width: 240,
            background: "#020617",
          },
        }}
      >
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" fontWeight="bold">
            Vulnuris
          </Typography>

          <List>
            <ListItem disablePadding>
              <ListItemButton>
                <DashboardIcon sx={{ mr: 1 }} />
                Dashboard
              </ListItemButton>
            </ListItem>
          </List>
        </Box>
      </Drawer>

      <Box sx={{ flex: 1, p: 4 }}>{children}</Box>
    </Box>
  );
}