package main

import (
	"encoding/json"
	"io"
	"os"
	"strings"
)

type Cred struct {
	ServerURL string `json:"ServerURL"`
	Username  string `json:"Username"`
	Secret    string `json:"Secret"`
}

func main() {
	b, err := io.ReadAll(os.Stdin)
	if err != nil {
		os.Exit(2)
	}
	hostname := strings.TrimSpace(string(b))

	// simulate an error for testing
	if hostname == "error.other.com" {
	    os.Stdout.Write([]byte("Error: Not found"))
		os.Exit(1)
	}

	cred := Cred{
		ServerURL: hostname,
		Username:  "user",
		Secret:    "password",
	}

	out, err := json.MarshalIndent(cred, "", "  ")
	if err != nil {
		os.Exit(2)
	}

	os.Stdout.Write(out)
	os.Stdout.Write([]byte("\n"))
}
