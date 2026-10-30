# Instruções de desenvolvimento — JJ Launcher

- Projeto Android nativo do zero, em Kotlin e Jetpack Compose.
- Prioridade: interface discreta, fluidez, acessibilidade e privacidade.
- Preserve o fluxo de launcher padrão (HOME) e o filtro de visibilidade restrito a atividades LAUNCHER.
- Não adicione internet, analytics, anúncios ou a permissão QUERY_ALL_PACKAGES sem requisito explícito e revisão.
- Mantenha a busca e as regras puras testáveis por testes unitários.
- Evite módulos, frameworks e abstrações prematuras.
- Antes de concluir uma alteração, execute `gradle :app:testDebugUnitTest :app:assembleDebug` ou relate por que não foi possível.
- Envie mudanças em branch e Pull Request; não faça merge sem autorização.

## Releases e atualizações

- Para distribuir atualizações, use apenas APKs assinados com a mesma chave release; não versione o keystore.
- Não use builds debug do CI como atualizações de produção (assinatura instável).
- Novas versões devem incrementar `JJ_RELEASE_VERSION` e resultar em `versionCode` crescente.
- Segredos de assinatura só no workflow de publicação manual a partir de main, nunca em PRs.
- O botão Atualizações abre a fonte oficial, enquanto Obtainium cuida da detecção e instalação com consentimento Android.
