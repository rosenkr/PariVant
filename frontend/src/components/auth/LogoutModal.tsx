import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import {
    Button,
    Dialog,
    DialogContent,
    IconButton,
    Stack,
    Typography,
} from "@mui/material";
import {useAuth} from "../../auth/AuthContext.tsx";


type Props = {
    open: boolean;
    onClose: () => void;
    onSuccess?: () => void;
};

export function LogoutModal({ open, onClose, onSuccess }: Props) {
    const { signOut } = useAuth();


    const handleLogout = () => {
        signOut();
        if (onSuccess) {
            onSuccess();
            return;
        }
        onClose();
    }
    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullWidth
            maxWidth="xs"
            scroll="body"
            slotProps={{
                paper: {
                    sx: (theme) => ({
                        borderRadius: 3,
                        backgroundColor: theme.appColors.auth.modalBackground,
                        border: `1px solid ${theme.appColors.auth.modalBorder}`,
                        boxShadow: theme.appColors.auth.modalShadow,
                    }),
                },
            }}
        >
            <DialogContent
                sx={{
                    p: { xs: 3, sm: 4 },
                }}
            >
                <Stack spacing={3}>
                    <Stack spacing={2} alignItems="center" sx={{ position: "relative" }}>
                        <IconButton
                            onClick={onClose}
                            aria-label="Close authentication dialog"
                            sx={{
                                position: "absolute",
                                top: -8,
                                right: -8,
                            }}
                        >
                            <CloseRoundedIcon />
                        </IconButton>

                        <Stack spacing={0.5} alignItems="center">
                            <Typography
                                variant="body1"
                                align="center"
                                sx={(theme) => ({
                                    color: theme.appColors.text.secondary,
                                })}
                            >
                                Are you sure you want to log out?
                            </Typography>
                            <Stack spacing={0.5} alignItems="center" direction="row">
                                <Button onClick={handleLogout}>
                                    Log out
                                </Button>
                                <Button onClick={onClose}>
                                    Cancel
                                </Button>
                            </Stack>
                        </Stack>
                    </Stack>
                </Stack>
            </DialogContent>
        </Dialog>
    );
}
