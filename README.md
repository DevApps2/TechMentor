# 📱 Projeto Etec leste

Projeto desenvolvido em **Kotlin** com o objetivo de criar um sistema acadêmico para cadastro e gerenciamento de alunos.

---

# 📋 Regras do Projeto

## 🌳 1. Branches

A `main` representa a **versão estável** do projeto.

Cada integrante deve trabalhar em uma branch própria.

### 📌 Padrão

| Tipo | Padrão |
|---|---|
| 🆕 Funcionalidade | `feature/nome-da-tarefa` |
| 🐛 Correção | `fix/nome-da-correcao` |
| 🎨 Visual | `style/nome-da-alteracao` |

### Exemplos

```text
feature/cadastro-aluno
feature/tela-login
feature/validacao-email
fix/erro-cadastro
style/ajuste-layout
```

> 🚫 **Não fazer alterações diretamente na `main`.**

---

## 🔄 2. Antes de começar uma tarefa

Sempre atualize a `main`:

```bash
git checkout main
git pull origin main
```

Depois crie sua branch:

```bash
git checkout -b feature/nome-da-tarefa
```

---

## 💻 3. Desenvolvimento

Cada integrante deve trabalhar na **tarefa atribuída**.

### Regras

- Evitar alterar arquivos desnecessários;
- Não modificar partes do projeto sem necessidade;
- Comunicar alterações que possam afetar outro integrante;
- Manter o código organizado.

> 🎯 **Quanto menor e mais específica a alteração, mais fácil será revisar.**

---

## 💾 4. Commits

Os commits devem explicar claramente o que foi alterado.

### Padrão

```text
tipo: descrição
```

### Tipos

| Tipo | Utilização |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de erro |
| `style` | Alteração visual/formatação |
| `refactor` | Alteração na estrutura do código |
| `docs` | Documentação |
| `test` | Testes |
| `chore` | Configurações/manutenção |

### ✅ Bons exemplos

```bash
git commit -m "feat: adiciona cadastro de aluno"
```

```bash
git commit -m "fix: corrige validação de idade"
```

```bash
git commit -m "style: ajusta layout da tela inicial"
```

### ❌ Evitar

```text
coisas
teste
mudanças
aaaa
funcionou
final
final2
```

---

## 📦 5. Enviando alterações

Depois de terminar uma alteração:

```bash
git status
```

```bash
git add .
```

```bash
git commit -m "feat: descrição"
```

```bash
git push origin nome-da-branch
```

---

## 🔀 6. Pull Request

Nenhuma alteração deve entrar na `main` sem passar por um **Pull Request (PR)**.

### Fluxo

```text
🌱 Branch
   ↓
💻 Desenvolvimento
   ↓
🧪 Testes
   ↓
💾 Commit
   ↓
☁️ Push
   ↓
🔀 Pull Request
   ↓
👀 Revisão
   ↓
✅ Aprovação
   ↓
🔗 Merge
   ↓
🌳 main
```

### O Pull Request deve informar:

- O que foi desenvolvido;
- Quais arquivos foram alterados;
- Alterações importantes;
- Se a funcionalidade foi testada.

---

## 👀 7. Revisão de código

Antes do merge, outro integrante deve revisar o código.

### Verificar:

- [ ] O código funciona;
- [ ] A tarefa foi concluída;
- [ ] Não existem erros evidentes;
- [ ] O código está organizado;
- [ ] Não existem alterações desnecessárias;
- [ ] Outras funcionalidades continuam funcionando.

---

## 🧪 8. Testes

Antes de abrir um Pull Request:

- [ ] O aplicativo inicia normalmente;
- [ ] A funcionalidade funciona;
- [ ] Os campos funcionam corretamente;
- [ ] As validações funcionam;
- [ ] Não existem erros no Logcat;
- [ ] Outras funcionalidades continuam funcionando.

---

## ⚠️ 9. Conflitos

Conflitos acontecem quando duas pessoas alteram a mesma parte de um arquivo.

```text
⚠️ CONFLICT
```

### Ao encontrar um conflito:

1. Identificar o conflito;
2. Analisar os dois códigos;
3. Combinar as alterações corretas;
4. Testar o projeto;
5. Fazer um novo commit.

Depois:

```bash
git add .
```

```bash
git commit -m "fix: resolve conflito"
```

> 🚫 Nunca apagar alterações de outro integrante sem verificar primeiro.

---

## 🔄 10. Atualização da branch

Se a `main` recebeu novas alterações enquanto você trabalha:

```bash
git checkout main
git pull origin main
```

Depois volte para sua branch:

```bash
git checkout nome-da-sua-branch
```

Mantenha sua branch atualizada para reduzir conflitos.

---

## 🗂️ 11. Organização dos arquivos

### Regras

- Manter a estrutura do projeto;
- Não criar arquivos desnecessários;
- Não duplicar arquivos sem necessidade;
- Remover arquivos de teste que não serão utilizados;
- Manter imagens e recursos organizados.

---

## 📝 12. Nomes de arquivos

Os nomes devem ser claros e seguir um padrão.

### ✅ Exemplos

```text
MainActivity.kt
Aluno.kt
AlunoRepository.kt
CadastroActivity.kt
```

### ❌ Evitar

```text
teste.kt
teste2.kt
coisa.kt
arquivoNovo.kt
final2.kt
```

---

## 🧹 13. Código

O código deve ser:

- ✅ Organizado;
- ✅ Legível;
- ✅ Simples;
- ✅ Fácil de entender;
- ✅ Comentado quando necessário.

### Evitar:

- Código morto;
- Variáveis desnecessárias;
- Código duplicado;
- Trechos antigos comentados.

### ❌ Exemplo

```kotlin
// código antigo
// val aluno = ...
// teste
```

### ✅ Preferir

Código limpo e funcional.

---

## 🔐 14. Informações sensíveis

Nunca enviar para o GitHub:

- 🔑 Senhas;
- 🎫 Tokens;
- 🔐 Chaves de API;
- 👤 Dados pessoais;
- 🔒 Credenciais.

Utilizar `.gitignore` quando necessário.

---

## ⚙️ 15. Configurações do projeto

Não alterar configurações importantes sem comunicar a equipe.

Isso inclui:

- Gradle;
- SDK;
- Dependências;
- Plugins;
- Configurações do projeto.

### Para adicionar uma biblioteca:

1. Informar a equipe;
2. Explicar a necessidade;
3. Verificar se já existe uma solução no projeto;
4. Testar o projeto após a alteração.

---

## 📱 16. Testar antes do Merge

Antes de solicitar o merge:

```text
✅ Projeto compila
✅ Aplicativo inicia
✅ Funcionalidade funciona
✅ Não apresenta erros
✅ Outras funcionalidades continuam funcionando
```

---

## 🚨 17. Force Push

Evitar:

```bash
git push --force
```

> ⚠️ O `force push` pode sobrescrever o histórico da branch e causar perda de alterações.

---

Adiciona esta seção depois de Pull Request:

---

## 📝 18. Issues

As **Issues** do GitHub serão utilizadas para organizar e acompanhar as tarefas e problemas do projeto.

### 🎯 Para que usar

- 🆕 Criar tarefas;
- 🐛 Registrar problemas e bugs;
- 💡 Registrar melhorias ou ideias;
- 📌 Acompanhar o andamento das atividades.

### 📋 Padrão da Issue

Cada Issue deve conter:

- **Título:** descrição curta e objetiva;
- **Descrição:** explicar o que precisa ser feito;
- **Responsável:** integrante responsável pela tarefa;
- **Labels:** categoria da tarefa;
- **Status:** acompanhar o andamento.

### 🏷️ Labels

| Label | Utilização |
|---|---|
| `feature` | Nova funcionalidade |
| `bug` | Correção de erro |
| `style` | Alteração visual |
| `documentation` | Documentação |
| `enhancement` | Melhoria |
| `task` | Tarefa geral |

### 🔄 Fluxo

```text
📝 Issue criada
      ↓
👤 Responsável definido
      ↓
🌱 Branch criada
      ↓
💻 Desenvolvimento
      ↓
🧪 Testes
      ↓
🔀 Pull Request
      ↓
✅ Merge
      ↓
✔️ Issue fechada
```

### 🔗 Issue + Branch + Pull Request

Sempre que possível, relacionar a branch e o Pull Request à Issue correspondente.

Exemplo:

```text
Issue #12
"Implementar cadastro de aluno"

        ↓

feature/cadastro-aluno

        ↓

Pull Request #18

        ↓

Merge na main

        ↓

Issue #12 fechada
```

> 📌 **Toda tarefa relevante deve possuir uma Issue antes de começar o desenvolvimento.**

## 🧑‍💻 19. Responsabilidade de cada integrante

Cada integrante é responsável por:

- Desenvolver sua tarefa;
- Manter sua branch atualizada;
- Fazer commits organizados;
- Testar suas alterações;
- Criar o Pull Request;
- Corrigir problemas encontrados na revisão;
- Comunicar conflitos e problemas à equipe.

---

## 🤝 10. Comunicação

Antes de alterar algo que possa afetar o trabalho de outro integrante, comunicar a equipe.

Principalmente:

```text
MainActivity
Banco de dados
Dependências
Configurações
Classes compartilhadas
Estrutura de pastas
```

---

## 🏁 21. Finalização de uma tarefa

Uma tarefa é considerada concluída quando:

```text
☑ Desenvolvimento concluído
☑ Código testado
☑ Commit realizado
☑ Branch enviada
☑ Pull Request criado
☑ Código revisado
☑ Correções realizadas
☑ Pull Request aprovado
☑ Merge realizado
```

---

# 🔥 Fluxo Oficial

```text
🌳 Atualizar main
       ↓
🌱 Criar branch
       ↓
💻 Desenvolver
       ↓
🧪 Testar
       ↓
💾 Commit
       ↓
☁️ Push
       ↓
🔀 Pull Request
       ↓
👀 Revisão
       ↓
🔧 Correções
       ↓
✅ Aprovação
       ↓
🔗 Merge
       ↓
🌳 Atualizar main
```

---

# 📌 Comandos principais

### 🔍 Ver alterações

```bash
git status
```

### 🌳 Ver branches

```bash
git branch
```

### 🔄 Atualizar a main

```bash
git checkout main
git pull origin main
```

### 🌱 Criar branch

```bash
git checkout -b feature/nome-da-tarefa
```

### ➕ Adicionar alterações

```bash
git add .
```

### 💾 Criar commit

```bash
git commit -m "feat: descrição"
```

### ☁️ Enviar branch

```bash
git push origin feature/nome-da-tarefa
```

---

# 🧠 Regra de Ouro

> 🌳 **`main` = versão estável**
>
> 🌱 **`feature/...` = desenvolvimento**
>
> 🔀 **Pull Request = revisão**
>
> 🔗 **Merge = entrada na `main`**
>
> 🧪 **Testar antes de fazer o merge**
