"use client";

import { GoogleLogin } from "@react-oauth/google";
import { handleGoogleLogin } from "@/app/actions/auth";

export function GoogleLoginButton({ nextPath }: { nextPath?: string }) {
  return (
    <div className="flex justify-center">
      <GoogleLogin
        onSuccess={async (credentialResponse) => {
          if (credentialResponse.credential) {
            await handleGoogleLogin(credentialResponse.credential, nextPath);
          }
        }}
        onError={() => {
          console.error("Login Failed");
        }}
        use_fedcm_for_button={false}
        use_fedcm_for_prompt={false}
        theme="outline"
        size="large"
        shape="rectangular"
        text="continue_with"
      />
    </div>
  );
}
