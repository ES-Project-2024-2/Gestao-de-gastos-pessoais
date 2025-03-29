import { Injectable } from '@angular/core';
import { User } from '../entity/user';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = environment.apiUrl + '/auth'; 
  private apiUrlDes = environment.apiUrl + '/admin'; 
  

  async register(newUser: User): Promise<User | null> {
    try {
      const response = await fetch(`${this.apiUrl}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newUser)
      });

      if (!response.ok) {
        throw new Error('Falha no registro');
      }

      return await response.json();
    } catch (error) {
      console.error('Erro ao registrar usuário:', error);
      return null;
    }
  }

  async login(email: string, password: string): Promise<boolean> {
    try {
      const response = await fetch(`${this.apiUrl}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });

      if (!response.ok) {
        throw new Error('Login falhou');
      }
      const data = await response.json();

      //salva o token JWT recebido
      localStorage.setItem('token', data.token);

      return true;
    } catch (error) {
      console.error('Erro ao fazer login:', error);
      return false;
    }
  }
  
  hasRole(requiredRole: string): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    try {
      const decodedToken: any = jwtDecode(token);
      return decodedToken.role === requiredRole;
    } catch (error) {
      console.error('Token error:', error);
      return false;
    }
  }
  
  logout(): void {
    localStorage.removeItem('token');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  isActive(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    try {
      const decodedToken: any = jwtDecode(token);
      if(decodedToken.estaAtivo == 'true'){
        return true;
      }
      return false;
    } catch (error) {
      console.error('Token error:', error);
      return false;
    }
  }

  getUserName(): string {
    const token = this.getToken();
    if (!token) {
      return '';
    }
    try {
      const decodedToken: any = jwtDecode(token);
      return decodedToken.sub;
    } catch (error) {
      console.error('Token error:', error);
      return '';
    }
  }

  getUserEmail(): string {
    const token = this.getToken();
    if (!token) {
      return '';
    }
    try {
      const decodedToken: any = jwtDecode(token);
      return decodedToken.email;
    } catch (error) {
      console.error('Token error:', error);
      return '';
    }
  }

  getUserId(): string {
    const token = this.getToken();
    if (!token) {
      return '';
    }
    try {
      const decodedToken: any = jwtDecode(token);
      return decodedToken.id;
    } catch (error) {
      console.error('Token error:', error);
      return '';
    }
  }

  async disableAccount(id: string): Promise<boolean> {
    try {
      const body = JSON.stringify({ estaAtivo: false });

      const response = await fetch(`${this.apiUrlDes}/users/${id}`, {
        method: 'PATCH',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getToken()}`
        },
        body: body
      });

      if (!response.ok) {
        throw new Error(`Falha na desativação da conta: ${response.status}`);
      }

      this.logout();
      return true;
    } catch (error) {
      console.error('Erro ao desativara a conta:', error);
      return false; 
    }
  }

}