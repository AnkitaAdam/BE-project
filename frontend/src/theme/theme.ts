import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
  palette: {
    mode: "dark",
    primary: {
      main: "#3B82F6",
    },
    secondary: {
      main: "#06B6D4",
    },
    background: {
      default: "#0F172A",
      paper: "#111827",
    },
  },
  typography: {
    fontFamily: "Inter, sans-serif",
    h4: {
      fontWeight: 700,
    },
  },
  shape: {
    borderRadius: 14,
  },
});