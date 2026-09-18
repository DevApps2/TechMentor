Projeto Kotlin — Guia de Trabalho com Git e GitHub

Este documento explica como o grupo deve trabalhar em conjunto no projeto Kotlin usando Git e GitHub.

1. Tecnologias

Kotlin

Git

GitHub

IntelliJ IDEA / Android Studio, conforme o projeto

2. Estrutura do projeto

A branch principal do projeto será:

main


A main deve conter apenas versões estáveis do projeto.

Cada integrante deve desenvolver suas tarefas em uma branch própria.

Exemplo:

main
│
├── feature/login
├── feature/cadastro
├── feature/tela-home
└── fix/erro-login

3. Antes de começar

Cada integrante precisa instalar o Git e configurar seu nome e e-mail:

git config --global user.name "Seu Nome"
git config --global user.email "seu@email.com"


Verifique:

git config --global --list


Depois, clone o projeto:

git clone URL_DO_REPOSITORIO


Entre na pasta:

cd nome-do-projeto

4. Entendendo o Git

Os principais estados são:

Arquivos
   ↓
git add
   ↓
Staging Area
   ↓
git commit
   ↓
Repositório local
   ↓
git push
   ↓
GitHub

git status

Mostra o estado atual do projeto:

git status


Use esse comando frequentemente.

5. Branches

Uma branch é uma linha de desenvolvimento independente.

Não é recomendado desenvolver diretamente na main.

Criar uma branch
git switch -c feature/nome-da-feature


Exemplo:

git switch -c feature/login


Podemos usar alguns padrões:

feature/     → nova funcionalidade
fix/         → correção de bug
refactor/    → reorganização do código
docs/        → documentação
test/        → testes


Exemplos:

feature/cadastro-usuario
feature/tela-login
fix/validacao-email
refactor/repository
test/login
docs/readme

6. Ver as branches

Para ver as branches locais:

git branch


Para ver também as branches remotas:

git branch -a

7. Trocar de branch
git switch nome-da-branch


Exemplo:

git switch main


Ou:

git switch feature/login

8. Criando uma nova feature

Suponha que alguém recebeu a tarefa:

Criar tela de login.

Primeiro, atualize a main:

git switch main
git pull origin main


Depois crie a branch:

git switch -c feature/login


Agora você pode trabalhar no código Kotlin.

Exemplo:

class LoginService {

    fun login(email: String, password: String): Boolean {
        return email.isNotBlank() && password.isNotBlank()
    }
}

9. Commit

Depois de realizar uma parte do trabalho:

git status


Adicione os arquivos:

git add .


Faça o commit:

git commit -m "feat: implementa login"


O commit deve explicar o que foi feito.

Exemplos:

git commit -m "feat: adiciona cadastro de usuario"
git commit -m "feat: cria tela inicial"
git commit -m "fix: corrige validacao de email"
git commit -m "test: adiciona testes do login"
git commit -m "refactor: reorganiza camada repository"


Evite commits como:

coisas
mudancas
teste
aaaa
final
agora vai

10. Enviar a branch para o GitHub

Depois do commit:

git push origin feature/login


Na primeira vez, também pode ser utilizado:

git push -u origin feature/login


Depois disso, normalmente basta:

git push

11. Pull Request (PR)

Depois que a feature estiver pronta, abra um Pull Request no GitHub.

O fluxo será:

feature/login
      ↓
     push
      ↓
   GitHub
      ↓
Pull Request
      ↓
Revisão
      ↓
Merge
      ↓
main


O Pull Request permite que outros integrantes revisem o código antes de ele entrar na main.

12. Revisão de código

Antes do merge, outro integrante deve verificar o código.

Algumas coisas para conferir:

O código funciona?

A feature realmente resolve a tarefa?

Existem erros óbvios?

Os nomes de classes e funções estão claros?

Existem testes quando necessários?

O código segue o padrão do projeto?

A alteração quebrou alguma funcionalidade existente?

Comentários devem ser feitos no Pull Request.

13. Atualizando sua branch

Enquanto você trabalha, outras pessoas podem alterar a main.

Por isso, antes de finalizar sua feature, atualize sua branch.

Uma forma simples:

git switch main
git pull origin main
git switch feature/login
git merge main


Se houver alterações, o Git tentará juntá-las à sua branch.

Outra possibilidade é utilizar rebase, mas para grupos iniciantes é recomendável primeiro dominar o fluxo com merge.

14. Merge

Quando o Pull Request for aprovado, a feature pode ser incorporada à main.

Exemplo:

main
  │
  ├───────────────┐
  │               │
  │         feature/login
  │               │
  │          desenvolvimento
  │               │
  └───────────────┘
          merge


Depois do merge:

git switch main
git pull origin main


A main estará atualizada.

15. Conflitos

Um conflito acontece quando duas pessoas modificam a mesma parte do código de maneiras incompatíveis.

Por exemplo:

Pessoa A:

val nome = "João"


Pessoa B:

val nome = "Maria"


Quando o Git não consegue decidir qual alteração deve permanecer, ele marca um conflito.

O arquivo pode ficar parecido com:

<<<<<<< HEAD
val nome = "João"
=======
val nome = "Maria"
>>>>>>> feature/outra-branch


O integrante precisa decidir qual código deve permanecer.

Depois de resolver:

git add .
git commit -m "fix: resolve conflito de merge"

16. Regra importante sobre conflitos

Não resolva conflitos simplesmente escolhendo "o meu código".

Converse com a pessoa que fez a outra alteração e entendam o que cada mudança fazia.

Principalmente em arquivos importantes como:

build.gradle.kts
settings.gradle.kts
AndroidManifest.xml
arquivos de configuração
classes utilizadas por várias features

17. Não trabalhar diretamente na main

Evitem fazer:

git switch main
# alterar código
git add .
git commit
git push


O fluxo recomendado é:

main
 ↓
criar branch
 ↓
desenvolver
 ↓
commit
 ↓
push
 ↓
Pull Request
 ↓
review
 ↓
merge
 ↓
main

18. Divisão das tarefas

O grupo deve dividir o projeto em tarefas.

Exemplo:

#1 Criar sistema de login
#2 Criar cadastro
#3 Criar tela inicial
#4 Criar banco de dados
#5 Criar testes
#6 Criar documentação


Cada tarefa pode gerar uma branch:

feature/login
feature/cadastro
feature/home
feature/database
test/login
docs/readme

19. Uma pessoa por branch

Como regra geral:

Pessoa A → feature/login
Pessoa B → feature/cadastro
Pessoa C → feature/home
Pessoa D → feature/database


Isso diminui bastante a chance de conflitos.

Se duas pessoas precisarem trabalhar na mesma feature, combinem antes como dividir o trabalho.

20. Commits pequenos

Prefira:

commit 1 → cria modelo User
commit 2 → cria UserRepository
commit 3 → adiciona validação
commit 4 → adiciona testes


Em vez de:

commit → fiz o sistema inteiro


Commits pequenos facilitam a revisão e a identificação de problemas.

21. Antes de abrir um Pull Request

Faça:

git status


Verifique se não existem alterações esquecidas.

Depois:

git switch main
git pull origin main


Volte para sua branch:

git switch feature/login


Atualize sua branch:

git merge main


Execute os testes do projeto.

Se tudo estiver funcionando:

git push


Depois abra o Pull Request.

22. Depois que o Pull Request for aprovado

Após o merge:

git switch main
git pull origin main


Agora sua main local está atualizada.

Para começar outra tarefa:

git switch -c feature/nova-feature

23. Comandos principais
Configuração
git config --global user.name "Seu Nome"
git config --global user.email "seu@email.com"

Clonar
git clone URL_DO_REPOSITORIO

Status
git status

Atualizar
git pull

Criar branch
git switch -c feature/minha-feature

Trocar branch
git switch nome-da-branch

Ver branches
git branch

Adicionar arquivos
git add .

Criar commit
git commit -m "feat: minha alteração"

Enviar para o GitHub
git push

Ver histórico
git log --oneline

Mesclar uma branch
git merge nome-da-branch

24. Convenção de commits

Podemos utilizar uma convenção simples:

feat: nova funcionalidade

fix: correção de bug

refactor: alteração estrutural sem mudar comportamento

test: criação ou alteração de testes

docs: documentação

chore: tarefas de manutenção/configuração


Exemplos:

git commit -m "feat: adiciona cadastro de usuario"
git commit -m "fix: corrige login invalido"
git commit -m "refactor: separa camada de servico"
git commit -m "test: adiciona testes do cadastro"
git commit -m "docs: atualiza README"
git commit -m "chore: atualiza dependencias"

25. Organização recomendada do GitHub

O repositório pode ser organizado com:

Issues
   ↓
Tarefas

Branches
   ↓
Desenvolvimento

Pull Requests
   ↓
Revisão

main
   ↓
Código integrado


O grupo pode utilizar Issues para registrar o que precisa ser feito.

Exemplo:

Issue #12
Título: Criar tela de login

Descrição:
- Criar campos de e-mail e senha
- Adicionar botão de login
- Validar campos
- Criar testes


A pessoa responsável cria:

feature/login


e desenvolve a tarefa.

26. Regra de ouro do grupo

Antes de começar a trabalhar:

git switch main
git pull origin main
git switch -c feature/minha-feature


Durante o desenvolvimento:

git status
git add .
git commit -m "feat: descrição da alteração"
git push


Quando terminar:

Pull Request
    ↓
Code Review
    ↓
Aprovado
    ↓
Merge
    ↓
main

27. Fluxo completo

O fluxo que todos devem seguir é:

1. Pegar uma Issue
        ↓
2. Atualizar a main
        ↓
3. Criar uma branch
        ↓
4. Desenvolver a feature
        ↓
5. Fazer commits
        ↓
6. Fazer push
        ↓
7. Abrir Pull Request
        ↓
8. Outro integrante revisar
        ↓
9. Corrigir o que for necessário
        ↓
10. Aprovar PR
        ↓
11. Fazer merge
        ↓
12. Atualizar a main
        ↓
13. Começar a próxima tarefa

Exemplo prático

João recebeu a tarefa de criar o login.

git switch main
git pull origin main

git switch -c feature/login


João desenvolve a funcionalidade.

Depois:

git status
git add .
git commit -m "feat: implementa login"
git push -u origin feature/login


João abre um Pull Request no GitHub.

Maria revisa o código.

Se estiver tudo certo, o PR é aprovado e entra na main.

João então atualiza seu repositório:

git switch main
git pull origin main


E está pronto para pegar outra tarefa.

28. Regra principal

Nunca façam push diretamente na main sem que essa seja a política combinada pelo grupo.

O padrão recomendado para o projeto é:

Issue
 ↓
Feature Branch
 ↓
Commit
 ↓
Push
 ↓
Pull Request
 ↓
Code Review
 ↓
Merge
 ↓
Main


Esse fluxo mantém o projeto organizado e permite que várias pessoas trabalhem simultaneamente sem precisar ficar enviando arquivos umas para as outras.
