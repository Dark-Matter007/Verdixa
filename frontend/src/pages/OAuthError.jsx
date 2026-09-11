import { useSearchParams } from "react-router-dom";
import { AuthState } from "./OAuthCallback";

const messages = {
  missing_verified_email: "Verdixa needs a verified email address to create or connect your account. Make a verified email available in GitHub, then try again.",
  oauth_failed: "We couldn’t complete your social sign-in. It may have been cancelled or is temporarily unavailable."
};
export default function OAuthError() { const [params] = useSearchParams(); return <AuthState error={messages[params.get("reason")] || messages.oauth_failed} />; }
