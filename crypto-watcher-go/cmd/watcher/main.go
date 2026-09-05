package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"cryptopay-gateway-watcher-go/internal/app"
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	logger := slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelInfo,
	}))

	application := app.New(logger)
	if err := application.Run(ctx); err != nil {
		logger.Error("watcher exited with error", "error", err)
		os.Exit(1)
	}
}
