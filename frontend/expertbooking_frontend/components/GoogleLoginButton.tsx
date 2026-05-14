"use client";

import { GoogleLogin } from "@react-oauth/google";
import { handleGoogleLogin } from "@/app/actions/auth";

export function GoogleLoginButton() {
  return (
    <div className="flex justify-center">
      <GoogleLogin
        onSuccess={async (credentialResponse) => {
          if (credentialResponse.credential) {
            await handleGoogleLogin(credentialResponse.credential);
          }
        }}
        onError={() => {
          console.error("Login Failed");
        }}
        theme="outline"
        size="large"
        shape="rectangular"
        text="continue_with"
      />
    </div>
  );
}
