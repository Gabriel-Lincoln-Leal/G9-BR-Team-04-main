import { HttpInterceptorFn } from '@angular/common/http';

const STORAGE_KEY = 'techmind_api_key';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/conteudo') && !request.url.startsWith('/users')) {
    return next(request);
  }

  let apiKey = sessionStorage.getItem(STORAGE_KEY);
  if (!apiKey) {
    apiKey = window.prompt('Informe a chave de acesso do TechMind:')?.trim() || null;
    if (apiKey) sessionStorage.setItem(STORAGE_KEY, apiKey);
  }

  return next(apiKey
    ? request.clone({ setHeaders: { Authorization: `Bearer ${apiKey}` } })
    : request);
};
