package by.ares.transaction_validation_service.mcp;

import by.ares.transaction_validation_service.dto.ExceededTransactionResponseDto;
import by.ares.transaction_validation_service.dto.ExpenseLimitDto;
import by.ares.transaction_validation_service.service.ExpenseLimitService;
import by.ares.transaction_validation_service.service.TransactionProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientMcpTools {

    private final ExpenseLimitService expenseLimitService;
    private final TransactionProcessingService transactionProcessingService;

    @Tool(description = "Fetch all historical expense limits for a specific client account number")
    public List<ExpenseLimitDto> getClientLimits(
            @ToolParam(description = "The client account number (account_from)") String accountFrom) {
        log.info("MCP Tool invocation: getClientLimits for account: {}", accountFrom);
        return expenseLimitService.getClientLimits(accountFrom);
    }

    @Tool(description = "Fetch all transactions that exceeded the monthly limit for a specific client account number")
    public List<ExceededTransactionResponseDto> getExceededTransactions(
            @ToolParam(description = "The client account number (account_from)") String accountFrom) {
        log.info("MCP Tool invocation: getExceededTransactions for account: {}", accountFrom);
        return transactionProcessingService.getExceededTransactions(accountFrom);
    }
}
