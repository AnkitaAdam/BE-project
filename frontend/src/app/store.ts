import { configureStore } from "@reduxjs/toolkit";
import logReducer from "../features/logs/logSlice";

export const store = configureStore({
  reducer: {
    logs: logReducer,
  },
});