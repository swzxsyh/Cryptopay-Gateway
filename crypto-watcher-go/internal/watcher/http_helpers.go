package watcher

import (
	"net/http"
	"os"
	"strings"
	"time"
)

func getenv(key string) string {
	return os.Getenv(key)
}

func defaultHTTPClient() *http.Client {
	return &http.Client{Timeout: 12 * time.Second}
}

func canonicalSet(values []string) map[string]struct{} {
	out := make(map[string]struct{}, len(values))
	for _, value := range values {
		key := canonicalAddress(value)
		if key != "" {
			out[key] = struct{}{}
		}
	}
	return out
}

func canonicalAddress(value string) string {
	value = strings.TrimSpace(value)
	if value == "" {
		return ""
	}
	return strings.ToLower(strings.TrimPrefix(value, "0x"))
}

func millisTime(value int64) time.Time {
	if value <= 0 {
		return time.Now().UTC()
	}
	if value > 1_000_000_000_000 {
		return time.UnixMilli(value).UTC()
	}
	return time.Unix(value, 0).UTC()
}
