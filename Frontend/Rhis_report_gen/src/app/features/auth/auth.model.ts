export interface LoginCredentials {
  email: string;
  password: string;
}

export interface CurrentUser {
  email: string;
  roles: readonly string[];
}
