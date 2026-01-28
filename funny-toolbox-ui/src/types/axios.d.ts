import 'axios';

declare module 'axios' {
  interface AxiosRequestConfig {
    skipAuthHandler?: boolean;
  }

  interface InternalAxiosRequestConfig {
    skipAuthHandler?: boolean;
  }
}
