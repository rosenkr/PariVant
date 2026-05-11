import type {AuthResponse} from "../types/auth.ts";
import {Typography} from "@mui/material";

type Props = {
    user: AuthResponse["user"];
};

export function UserGreeting({ user }: Props) {
    return <Typography>Hello {user.email}</Typography>
}