import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormsModule, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HomeService } from '../home.service';
import { Router } from '@angular/router';
import { Expense } from '../../entity/expense';

@Component({
  selector: 'app-expense',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
  <section class="expense-container">
    <button class ="home-button"(click)="home()">Home</button>
    <div class="left-section">
  <button class="criar-edit-trash" (click)="openModal('create')">Criar Despesa</button>
  <button 
    class="criar-edit-trash" 
    [class.cancel-mode]="isEditing" 
    (click)="toggleEditMode()">
    {{ isEditing ? 'Cancelar Edição' : 'Editar Despesa' }}
  </button>
  <button 
    class="criar-edit-trash" 
    [class.cancel-mode]="isRemoving" 
    (click)="toggleRemoveMode()">
    {{ isRemoving ? 'Cancelar Remoção' : 'Remover Despesa' }}
  </button>
</div>

    <div class="main-content">
    <div class="chart-container">
        <h2>Gráfico de Despesas</h2>
        <div class="date-filter">
          <label>Data Inicial:</label>
          <input type="date" [(ngModel)]="startDate" />

          <label>Data Final:</label>
          <input type="date" [(ngModel)]="endDate" />

          <button (click)="applyDateFilter()">Aplicar Filtro</button>
          <button (click)="clearFilter()">Limpar Filtro</button>
        </div>

        <svg id="expenseChart" width="300" height="300"></svg>

        <!-- Legenda Sincronizada -->
        <ul class="legend">
          <li *ngFor="let expense of filteredExpenses; let i = index">
            <span class="legend-color" [style.background]="getColor(i)"></span>
            {{ expense.categoria }} - R$ {{ expense.valor | number:'1.2-2' }}
          </li>
        </ul>
      </div>
<div class="chart-container">
  <h2>Despesas por Mês</h2>

  <div class="date-filter">
    <label>Mês Inicial:</label>
    <input type="month" [(ngModel)]="startMonth" />

    <label>Mês Final:</label>
    <input type="month" [(ngModel)]="endMonth" />

    <button (click)="applyMonthFilter()">Aplicar Filtro</button>
    <button (click)="clearMonthFilter()">Limpar Filtro</button>
  </div>

  <svg id="barChart" width="650" height="400" viewBox="0 0 650 400"></svg>
</div>


    <!-- Lista de Despesas -->
        <div class="expense-list">
          <h2>Lista de Despesas</h2>
          <ul>
            <li *ngFor="let expense of expenses">
              <div>
                <strong>Data:</strong> {{ expense.data | date:'dd/MM/yyyy' }} <br>
                <strong>Categoria:</strong> {{ expense.categoria }} <br>
                <strong>Valor:</strong> R$ {{ expense.valor | number:'1.2-2' }} <br>
                <strong>Destino do pagamento:</strong> {{ expense.destinoPagamento }} <br>
                <strong>Observações:</strong> {{ expense.observacoes || 'Nenhuma' }}
              </div>

              <!-- Mostrar botão de remoção apenas se o modo de remoção ou edição estiver ativo -->
              <button class="edit-remove" *ngIf="isEditing" (click)="openEditModal(expense)"><img src="assets/edit-bnt.png"></button>
              <button class="edit-remove" *ngIf="isRemoving" (click)="onSubmitRemove(expense.uuid!)"><img src="assets/trash-bnt.png"></button>
            </li>
          </ul>
        </div>

      <!-- Modal Criar Despesa -->
      <div [ngClass]="{'modal': true, 'show-modal': modalType === 'create'}">
        <div class="modal-content">
          <button class="close" (click)="closeModal()">&times;</button>
          <h2>Criar Despesa</h2>
          <form [formGroup]="createExpenseForm" (ngSubmit)="onSubmitCreate()">
            <label for="data">Data</label>
            <input type="date" formControlName="data" placeholder="Digite a data"/>

            <label for="categoria">Categoria</label>
            <div>
              <select id="categoria" formControlName="categoria" required>
                <option value="" disabled selected>Selecione uma categoria</option>
                <option value="ALIMENTACAO">Alimentação</option>
                <option value="MORADIA">Moradia</option>
                <option value="TRANSPORTE">Transporte</option>
                <option value="LAZER">Lazer</option>
              </select>
            </div>

            <label for="valor">Valor</label>
            <input type="text" formControlName="valor"/>

            <label for="destinoPagamento">Destino do pagamento</label>
            <input type="text" formControlName="destinoPagamento"/>

            <label>Observação</label>
            <input type="text" formControlName="observacoes"/>

            <button type="submit" [disabled]="createExpenseForm.invalid">Criar Despesa</button>
          </form>
        </div>
      </div>

      <!-- Modal Editar Despesa -->
      <div [ngClass]="{'modal': true, 'show-modal': modalType === 'edit'}">
        <div class="modal-content">
          <button class="close" (click)="closeModal()">&times;</button>
          <h2>Editar Despesa</h2>
         <form [formGroup]="editExpenseForm" (ngSubmit)="onSubmitEdit(editingExpenseId!)">
            <label>Data</label>
            <input type="date" formControlName="data"/>

            <label>Categoria</label>
            <div>
              <select id="categoria" formControlName="categoria" required>
                <option value="" disabled selected>Selecione uma categoria</option>
                <option value="ALIMENTACAO">Alimentação</option>
                <option value="MORADIA">Moradia</option>
                <option value="TRANSPORTE">Transporte</option>
                <option value="LAZER">Lazer</option>
              </select>
            </div>

            <label>Valor</label>
            <input type="number" formControlName="valor"/>

            <label>Destino</label>
            <input type="text" formControlName="destinoPagamento"/>

            <label>Observação</label>
            <input type="text" formControlName="observacoes"/>

            <button type="submit" [disabled]="editExpenseForm.invalid">Salvar Alterações</button>
          </form>
        </div>
      </div>
    </div>
  </section>
  `,
  styleUrls: ['expense.component.css']
})
export class ExpenseComponent {
  title = 'expense'
  startMonth: string = ''; // Mês inicial selecionado
  endMonth: string = '';   // Mês final selecionado
  filteredBarData: Expense[] = [];  // Lista filtrada para o gráfico de barras


  isRemoving = false;
  isEditing = false;
  editingExpenseId: string | null = null;
  modalType: 'create' | 'edit' | null = null;

  expenses: Expense[] = []; // Lista de despesas
  filteredExpenses: Expense[] = []; // Despesas filtradas
  pieChartStyles: any[] = []; // Lista de estilos CSS para o gráfico

  startDate: string = '';  // Data inicial
  endDate: string = '';    // Data final

  private homeService = inject(HomeService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  createExpenseForm: FormGroup = this.fb.group({
    data: ['', Validators.required],
    categoria: ['', Validators.required],
    valor: ['', Validators.required],
    destinoPagamento: ['', Validators.required],
    observacoes: ['', Validators.required],
  });

  editExpenseForm: FormGroup = this.fb.group({
    data: ['', Validators.required],
    categoria: ['', Validators.required],
    valor: ['', Validators.required],
    destinoPagamento: ['', Validators.required],
    observacoes: ['', Validators.required],
  });

  async ngOnInit() {
    await this.carregarDespesas();
  }

  refreshPage() {
    window.location.reload();
  }

  toggleRemoveMode() {
    this.isRemoving = !this.isRemoving;
    if (this.isEditing) this.isRemoving = false;
  }


  toggleEditMode() {
    this.isEditing = !this.isEditing;
    if (this.isRemoving) {
      this.isEditing = false; 
    }
  }

  async carregarDespesas() {
    const response = await this.homeService.getExpenses();
    if (response) {
      this.expenses = response;
      this.filteredExpenses = [...this.expenses]; // Inicialmente, exibe todas as despesas
      this.gerarGraficoPizza();
      this.gerarGraficoBarras();
    }
  }
  clearMonthFilter() {
    this.startMonth = '';
    this.endMonth = '';
    this.filteredBarData = [...this.expenses]; // Restaura todas as despesas
    this.gerarGraficoBarras(); // Atualiza o gráfico
  }
  

  applyMonthFilter() {
    if (!this.startMonth || !this.endMonth) return;
  
    const startDate = new Date(`${this.startMonth}-01`);
    const endDate = new Date(`${this.endMonth}-31`);
  
   
    this.filteredBarData = this.expenses.filter(expense => {
      const expenseDate = new Date(expense.data);
      return expenseDate >= startDate && expenseDate <= endDate;
    });
  
    this.gerarGraficoBarras(); // Atualiza o gráfico de barras apenas
  }

  getMonthIndex(month: string): number {
    const months = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];
    return months.indexOf(month.toLowerCase());
  }
  

  gerarGraficoBarras() {
    const svg = document.getElementById('barChart') as unknown as SVGSVGElement;
    if (!svg) return;
  
    svg.innerHTML = ''; // Limpa o gráfico antes de redesenhar
  
    const data = this.filteredBarData.length > 0 ? this.filteredBarData : this.expenses;
    if (data.length === 0) return;
  
   
    const totals: { [key: string]: number } = {};
    data.forEach(expense => {
      const date = new Date(expense.data);
      const monthYear = `${date.toLocaleString('default', { month: 'short' })} ${date.getFullYear()}`;
  
      if (!totals[monthYear]) {
        totals[monthYear] = 0;
      }
      totals[monthYear] += expense.valor; 
    });
  
    
    const months = Object.keys(totals)
    .map(month => ({ 
      month, 
      date: new Date(Number(month.split(' ')[1]), this.getMonthIndex(month.split(' ')[0])) 
  }))
  
      .sort((a, b) => b.date.getTime() - a.date.getTime()) 
      .map(item => item.month);
  
    const values = months.map(m => totals[m]);
  
    const maxValor = Math.max(...values);
    const barWidth = 25;  
    const barSpacing = 50; 
    const startX = 50;
    const startY = 350;
    const chartHeight = 200;
  
    months.forEach((month, index) => {
      const totalValue = totals[month];
      const barHeight = (totalValue / maxValor) * chartHeight;
      const x = startX + index * barSpacing;
      const y = startY - barHeight;
  
      // Criando a barra
      const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
      rect.setAttribute("x", x.toString());
      rect.setAttribute("y", y.toString());
      rect.setAttribute("width", barWidth.toString());
      rect.setAttribute("height", barHeight.toString());
      rect.setAttribute("fill", this.getColor(index));
  
      svg.appendChild(rect);
  
      
      const text = document.createElementNS("http://www.w3.org/2000/svg", "text");
      text.setAttribute("x", (x + barWidth / 2).toString());
      text.setAttribute("y", (startY + 15).toString()); 
      text.setAttribute("text-anchor", "middle");
      text.setAttribute("font-size", "11px");
      text.textContent = month;
  
      svg.appendChild(text);
    });
  
    
    const xAxis = document.createElementNS("http://www.w3.org/2000/svg", "line");
    xAxis.setAttribute("x1", "40");
    xAxis.setAttribute("y1", startY.toString());
    xAxis.setAttribute("x2", (months.length * barSpacing + 50).toString());
    xAxis.setAttribute("y2", startY.toString());
    xAxis.setAttribute("stroke", "black");
    xAxis.setAttribute("stroke-width", "2");
    svg.appendChild(xAxis);
  }
  
  
   agruparDespesasPorMes(): { [key: string]: number } {
    const totais: { [key: string]: number } = {};

    this.filteredExpenses.forEach(expense => {
      const date = new Date(expense.data);
      const monthYear = `${date.toLocaleString('default', { month: 'short' })} ${date.getFullYear()}`;

      if (!totais[monthYear]) {
        totais[monthYear] = 0;
      }
      totais[monthYear] += Number(expense.valor);
    });

    return totais;
  }


  applyDateFilter() {
    if (!this.startDate || !this.endDate) {
      this.filteredExpenses = [...this.expenses]; // Exibe todas as despesas se não houver filtro
    } else {
      const start = new Date(this.startDate);
      const end = new Date(this.endDate);

      this.filteredExpenses = this.expenses.filter(expense => {
        const expenseDate = new Date(expense.data);
        return expenseDate >= start && expenseDate <= end;
      });
    }
    this.gerarGraficoPizza();
  }
  
  clearFilter() {
    this.startDate = '';
    this.endDate = '';
    this.filteredExpenses = [...this.expenses]; // Restaurar todas as despesas
    this.gerarGraficoPizza();
  }
  
  

  gerarGraficoPizza() {
    const svg = document.getElementById('expenseChart') as unknown as SVGSVGElement;
    if (!svg) return;

    svg.innerHTML = ''; // Limpa o gráfico antes de redesenhá-lo

    if (this.filteredExpenses.length === 0) return;

    const total = this.filteredExpenses.reduce((sum, expense) => sum + expense.valor, 0);
    let startAngle = 0;
    const radius = 100;
    const centerX = 150, centerY = 150;

    this.filteredExpenses.forEach((expense, index) => {
      const sliceAngle = (expense.valor / total) * 2 * Math.PI;
      const endAngle = startAngle + sliceAngle;

      const x1 = centerX + radius * Math.cos(startAngle);
      const y1 = centerY + radius * Math.sin(startAngle);
      const x2 = centerX + radius * Math.cos(endAngle);
      const y2 = centerY + radius * Math.sin(endAngle);

      const largeArcFlag = sliceAngle > Math.PI ? 1 : 0;
      const pathData = `M ${centerX} ${centerY} L ${x1} ${y1} A ${radius} ${radius} 0 ${largeArcFlag} 1 ${x2} ${y2} Z`;

      const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
      path.setAttribute('d', pathData);
      path.setAttribute('fill', this.getColor(index));
      svg.appendChild(path);

      startAngle = endAngle;
    });
  }

  getColor(index: number): string {
    const colors = ["#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF", 
    "#FF9F40", "#D4AF37", "#8A2BE2", "#20B2AA", "#DC143C",
    "#FFD700", "#4682B4", "#32CD32", "#FF4500", "#6A5ACD",
    "#008080", "#8B0000", "#556B2F", "#D2691E", "#1E90FF"];
    return colors[index % colors.length];
  }

  // 🔹 Agrupar despesas por categoria
  agruparDespesasPorCategoria(): { [key: string]: number } {
    const totais: { [key: string]: number } = {};

    this.expenses.forEach(expense => {
      const categoria = expense.categoria;
      const valor = Number(expense.valor);

      if (totais[categoria]) {
        totais[categoria] += valor;
      } else {
        totais[categoria] = valor;
      }
    });

    return totais;
  }

  

  // Função para abrir o modal com base no tipo
  openModal(type: 'create' | 'edit') {
    this.modalType = type;
  }

  // Fechar o modal
  closeModal() {
    this.modalType = null;
  }

  // Envio para criar a despesa
  onSubmitCreate() {
    if (this.createExpenseForm.valid) {
      const { data, categoria, valor, destinoPagamento, observacoes } = this.createExpenseForm.value;
      const newExpense: Expense = { data, categoria, valor, destinoPagamento, observacoes };
      this.homeService.createExpense(newExpense)
        .then(() => {
          alert('Despesa criada com sucesso!');
        })
        .catch(err => alert('Erro ao criar despesa: ' + err));
        this.refreshPage();
    }
  }

  // Envio para editar a despesa
  onSubmitEdit(id: string) {
    if (this.editExpenseForm.valid) {
      const {data, categoria, valor, destinoPagamento, observacoes } = this.editExpenseForm.value;
      const editExpense: Expense = { data, categoria, valor, destinoPagamento, observacoes };

      this.homeService.updateExpense(id, editExpense)
      .then(() => {
        alert('Despesa atualizada com sucesso!');
      })
      .catch(err => alert('Erro ao atualizar despesa: ' + err));
    }
    this.refreshPage();
  }
  
  openEditModal(expense: Expense) {
      this.modalType = 'edit';
      this.editingExpenseId = expense.uuid!;

      this.editExpenseForm.setValue({
        data: expense.data,
        categoria: expense.categoria,
        valor: expense.valor,
        destinoPagamento: expense.destinoPagamento,
        observacoes: expense.observacoes
      });
    }
  

  // Envio para remover a despesa
  onSubmitRemove(id: string) {
    this.homeService.removeExpense(id).then(() => {
      alert('Despesa removida com sucesso!')
    })
    .catch(err => alert('Error removing expense: ' + err));
    this.refreshPage(); 
}

  home() {
    this.router.navigate(['/home']);
  }
}

