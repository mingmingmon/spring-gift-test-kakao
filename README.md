# spring-gift-test

## 테스트 실행 방법

### Cucumber 테스트만 실행

```bash
./gradlew test --tests "gift.cucumber.CucumberTest"
```

### 전체 테스트 실행 (기존 테스트 + Cucumber)

```bash
./gradlew test
```

### HTML 리포트 확인

테스트 실행 후 Feature/시나리오/Step별 성공·실패 상태를 시각적으로 확인할 수 있다.

```bash
open build/reports/cucumber.html
```